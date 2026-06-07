package dev.iamrat.auth.login.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptGuard {

    private final LoginAttemptStore loginAttemptStore;
    private final LoginProtectionProperties properties;

    public void guard(String username, String clientIp) {
        if (!properties.isEnabled() || isBlank(username)) {
            return;
        }

        try {
            String normalizedUsername = normalize(username);
            if (loginAttemptStore.hasLock(normalizedUsername)) {
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }

            incrementWithinLimit(
                loginAttemptStore.incrementUserRate(
                    normalizedUsername,
                    properties.getRateLimitWindowSeconds()
                ),
                properties.getUserLimitPerWindow()
            );

            if (!isBlank(clientIp)) {
                incrementWithinLimit(
                    loginAttemptStore.incrementIpRate(
                        clientIp,
                        properties.getRateLimitWindowSeconds()
                    ),
                    properties.getIpLimitPerWindow()
                );
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw failClosed("로그인 요청 가드 저장소 장애", username, clientIp, e);
        }
    }

    public void recordFailure(String username) {
        if (!properties.isEnabled() || isBlank(username)) {
            return;
        }

        try {
            String normalizedUsername = normalize(username);
            Long failures = loginAttemptStore.incrementFailure(
                normalizedUsername,
                properties.getFailureWindowSeconds()
            );

            if (failures != null && failures >= properties.getFailureLimit()) {
                loginAttemptStore.lock(normalizedUsername, properties.getLockSeconds());
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw failClosed("로그인 실패 기록 저장소 장애", username, null, e);
        }
    }

    public void clearFailure(String username) {
        if (!properties.isEnabled() || isBlank(username)) {
            return;
        }

        try {
            String normalizedUsername = normalize(username);
            loginAttemptStore.clearFailureAndLock(normalizedUsername);
        } catch (Exception e) {
            log.warn("로그인 성공 후 실패 기록 초기화 실패: username={}", username, e);
        }
    }

    private CustomException failClosed(String message, String username, String clientIp, Exception cause) {
        log.warn("{}: username={} clientIp={}", message, username, clientIp, cause);
        return new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    private void incrementWithinLimit(Long count, long limit) {
        if (count != null && count > limit) {
            throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
