package dev.iamrat.support.redis;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisGuardOperations {
    private static final String MARKER_VALUE = "1";

    private final RedisTemplate<String, String> redisTemplate;

    public Long incrementWithExpiry(String key, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count;
    }

    public boolean markIfAbsent(String key, long ttlSeconds) {
        Boolean marked = redisTemplate.opsForValue()
            .setIfAbsent(key, MARKER_VALUE, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(marked);
    }

    public void mark(String key, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, MARKER_VALUE, ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }
}
