package dev.iamrat.auth.login.infrastructure.redis;

import dev.iamrat.auth.login.application.LoginAttemptStore;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisLoginAttemptStore implements LoginAttemptStore {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean hasLock(String normalizedUsername) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisLoginAttemptKeys.lockKey(normalizedUsername)));
    }

    @Override
    public Long incrementUserRate(String normalizedUsername, long windowSeconds) {
        return incrementWithExpiry(RedisLoginAttemptKeys.usernameRateKey(normalizedUsername), windowSeconds);
    }

    @Override
    public Long incrementIpRate(String clientIp, long windowSeconds) {
        return incrementWithExpiry(RedisLoginAttemptKeys.ipRateKey(clientIp), windowSeconds);
    }

    @Override
    public Long incrementFailure(String normalizedUsername, long windowSeconds) {
        return incrementWithExpiry(RedisLoginAttemptKeys.failureKey(normalizedUsername), windowSeconds);
    }

    @Override
    public void lock(String normalizedUsername, long lockSeconds) {
        redisTemplate.opsForValue()
            .set(RedisLoginAttemptKeys.lockKey(normalizedUsername), "1", lockSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void clearFailureAndLock(String normalizedUsername) {
        redisTemplate.delete(List.of(
            RedisLoginAttemptKeys.failureKey(normalizedUsername),
            RedisLoginAttemptKeys.lockKey(normalizedUsername)
        ));
    }

    private Long incrementWithExpiry(String key, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count;
    }
}
