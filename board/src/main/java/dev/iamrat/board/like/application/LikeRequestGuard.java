package dev.iamrat.board.like.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeRequestGuard {
    private static final long RATE_LIMIT_PER_MINUTE = 30;

    private final LikeRequestWindow likeRequestWindow;

    public void guardPostLike(Long postId, Long accountId) {
        guard("post", postId, accountId, "like");
    }

    public void guardPostUnlike(Long postId, Long accountId) {
        guard("post", postId, accountId, "unlike");
    }

    public void guardCommentLike(Long commentId, Long accountId) {
        guard("comment", commentId, accountId, "like");
    }

    public void guardCommentUnlike(Long commentId, Long accountId) {
        guard("comment", commentId, accountId, "unlike");
    }

    private void guard(String targetType, Long entityId, Long accountId, String action) {
        if (entityId == null || accountId == null) {
            return;
        }

        try {
            if (!likeRequestWindow.markCooldownIfAbsent(targetType, entityId, accountId, action)) {
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }

            Long requestCount = likeRequestWindow.incrementRateCount(accountId);
            if (requestCount != null && requestCount == 1L) {
                likeRequestWindow.startRateWindow(accountId);
            }
            if (requestCount != null && requestCount > RATE_LIMIT_PER_MINUTE) {
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("좋아요 요청 가드 우회: action={} targetType={} entityId={} accountId={}", action, targetType, entityId, accountId, e);
        }
    }
}
