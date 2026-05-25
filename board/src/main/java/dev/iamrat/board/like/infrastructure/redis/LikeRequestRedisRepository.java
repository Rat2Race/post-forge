package dev.iamrat.board.like.infrastructure.redis;

import dev.iamrat.board.like.application.LikeRequestWindow;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LikeRequestRedisRepository implements LikeRequestWindow {

    private static final long COOLDOWN_SECONDS = 1;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean markCooldownIfAbsent(String targetType, Long entityId, Long accountId, String action) {
        Boolean cooldownAllowed = redisTemplate.opsForValue()
            .setIfAbsent(
                LikeRequestRedisKeys.cooldownKey(targetType, entityId, accountId, action),
                "1",
                COOLDOWN_SECONDS,
                TimeUnit.SECONDS
            );
        return !Boolean.FALSE.equals(cooldownAllowed);
    }

    @Override
    public Long incrementRateCount(Long accountId) {
        return redisTemplate.opsForValue().increment(LikeRequestRedisKeys.rateKey(accountId));
    }

    @Override
    public void startRateWindow(Long accountId) {
        redisTemplate.expire(LikeRequestRedisKeys.rateKey(accountId), RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
    }
}
