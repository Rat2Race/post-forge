package dev.iamrat.like.support;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeRequestGuard {
    private static final long COOLDOWN_SECONDS = 1;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;
    private static final long RATE_LIMIT_PER_MINUTE = 30;

    private final RedisTemplate<String, String> redisTemplate;

    public void guardPostLike(Long postId, String userId) {
        guard("post", postId, userId, "like");
    }

    public void guardPostUnlike(Long postId, String userId) {
        guard("post", postId, userId, "unlike");
    }

    public void guardCommentLike(Long commentId, String userId) {
        guard("comment", commentId, userId, "like");
    }

    public void guardCommentUnlike(Long commentId, String userId) {
        guard("comment", commentId, userId, "unlike");
    }

    private void guard(String targetType, Long entityId, String userId, String action) {
        if (entityId == null || userId == null || userId.isBlank()) {
            return;
        }

        try {
            String cooldownKey = "like:cooldown:" + targetType + ":" + action + ":" + entityId + ":" + userId;
            Boolean cooldownAllowed = redisTemplate.opsForValue()
                    .setIfAbsent(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(cooldownAllowed)) {
                throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
            }

            String rateKey = "like:rate:" + userId;
            Long requestCount = redisTemplate.opsForValue().increment(rateKey);
            if (requestCount != null && requestCount == 1L) {
                redisTemplate.expire(rateKey, RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            if (requestCount != null && requestCount > RATE_LIMIT_PER_MINUTE) {
                throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("좋아요 요청 가드 우회: action={} targetType={} entityId={} userId={}", action, targetType, entityId, userId, e);
        }
    }
}
