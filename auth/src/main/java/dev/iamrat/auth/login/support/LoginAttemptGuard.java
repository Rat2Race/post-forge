package dev.iamrat.auth.login.support;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptGuard {

    private final RedisTemplate<String, String> redisTemplate;
    private final LoginProtectionProperties properties;

    public void guard(String userId, String clientIp) {
        if (!properties.isEnabled() || isBlank(userId)) {
            return;
        }

        try {
            String normalizedUserId = normalize(userId);
            String lockKey = lockKey(normalizedUserId);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }

            incrementWithinLimit(
                userRateKey(normalizedUserId),
                properties.getRateLimitWindowSeconds(),
                properties.getUserLimitPerWindow()
            );

            if (!isBlank(clientIp)) {
                incrementWithinLimit(
                    ipRateKey(clientIp),
                    properties.getRateLimitWindowSeconds(),
                    properties.getIpLimitPerWindow()
                );
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("로그인 요청 가드 우회: userId={} clientIp={}", userId, clientIp, e);
        }
    }

    public void recordFailure(String userId) {
        if (!properties.isEnabled() || isBlank(userId)) {
            return;
        }

        try {
            String normalizedUserId = normalize(userId);
            Long failures = incrementWithExpiry(
                failureKey(normalizedUserId),
                properties.getFailureWindowSeconds()
            );

            if (failures != null && failures >= properties.getFailureLimit()) {
                redisTemplate.opsForValue()
                    .set(lockKey(normalizedUserId), "1", properties.getLockSeconds(), TimeUnit.SECONDS);
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("로그인 실패 기록 우회: userId={}", userId, e);
        }
    }

    public void clearFailure(String userId) {
        if (!properties.isEnabled() || isBlank(userId)) {
            return;
        }

        try {
            String normalizedUserId = normalize(userId);
            redisTemplate.delete(List.of(failureKey(normalizedUserId), lockKey(normalizedUserId)));
        } catch (Exception e) {
            log.warn("로그인 실패 기록 초기화 우회: userId={}", userId, e);
        }
    }

    private void incrementWithinLimit(String key, long windowSeconds, long limit) {
        Long count = incrementWithExpiry(key, windowSeconds);
        if (count != null && count > limit) {
            throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private Long incrementWithExpiry(String key, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String userRateKey(String normalizedUserId) {
        return "auth:login:rate:user:" + normalizedUserId;
    }

    private String ipRateKey(String clientIp) {
        return "auth:login:rate:ip:" + clientIp;
    }

    private String failureKey(String normalizedUserId) {
        return "auth:login:fail:" + normalizedUserId;
    }

    private String lockKey(String normalizedUserId) {
        return "auth:login:lock:" + normalizedUserId;
    }
}
