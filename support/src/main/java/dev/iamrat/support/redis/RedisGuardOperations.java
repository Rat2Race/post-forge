package dev.iamrat.support.redis;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisGuardOperations {
    private static final String MARKER_VALUE = "1";
    // INCR와 만료 설정을 한 번에 실행한다. 따로 보내면 그 사이 연결이 끊길 때 만료 없는 카운터가 남아 그 키의 요청이 영원히 막힌다.
    // 첫 증가가 아니라 "만료가 없을 때" 거는 것은 예전 결함으로 남은 키도 다음 증가에서 고치기 위해서다.
    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
        "local count = redis.call('INCR', KEYS[1]) "
            + "if redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
            + "return count",
        Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    public Long incrementWithExpiry(String key, long windowSeconds) {
        return redisTemplate.execute(INCREMENT_WITH_EXPIRY, List.of(key), Long.toString(windowSeconds));
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
