package dev.iamrat.board.like.infrastructure.redis;

import dev.iamrat.board.like.application.LikeRequestWindow;
import dev.iamrat.support.redis.RedisGuardOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LikeRequestRedisRepository implements LikeRequestWindow {

    private static final long COOLDOWN_SECONDS = 1;

    private final RedisGuardOperations redisGuardOperations;

    @Override
    public boolean markCooldownIfAbsent(String targetType, Long entityId, Long accountId, String action) {
        return redisGuardOperations.markIfAbsent(
            LikeRequestRedisKeys.cooldownKey(targetType, entityId, accountId, action),
            COOLDOWN_SECONDS
        );
    }

    @Override
    public Long incrementRateCount(Long accountId, long windowSeconds) {
        return redisGuardOperations.incrementWithExpiry(LikeRequestRedisKeys.rateKey(accountId), windowSeconds);
    }
}
