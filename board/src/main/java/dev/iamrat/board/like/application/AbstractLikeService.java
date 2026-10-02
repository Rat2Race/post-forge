package dev.iamrat.board.like.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLikeService {

    // 행을 실제로 넣거나 지운 요청만 카운터를 1 옮긴다. 이미 그 상태면 아무것도 바꾸지 않고 현재 수를 돌려준다(멱등).
    protected LikeResult likeTarget(Long targetId, Long accountId) {
        validateTargetAndAccount(targetId, accountId);

        if (insertLikeIfAbsent(targetId, accountId)) {
            addLikeCount(targetId, 1);
        }

        return new LikeResult(true, countByTargetId(targetId));
    }

    protected LikeResult unlikeTarget(Long targetId, Long accountId) {
        validateTargetAndAccount(targetId, accountId);

        if (deleteLike(targetId, accountId)) {
            addLikeCount(targetId, -1);
        }

        return new LikeResult(false, countByTargetId(targetId));
    }

    protected Map<Long, Long> getLikeCountMap(List<Long> targetIds) {
        if (targetIds == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        if (targetIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>();
        for (Long targetId : targetIds) {
            result.put(targetId, 0L);
        }

        for (Object[] row : countByTargetIds(targetIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }

        return result;
    }

    protected Set<Long> getLikedTargetIds(List<Long> targetIds, Long accountId) {
        if (targetIds == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        if (accountId == null || targetIds.isEmpty()) {
            return Collections.emptySet();
        }

        return findLikedTargetIds(accountId, targetIds);
    }

    private void validateTargetAndAccount(Long targetId, Long accountId) {
        if (targetId == null || accountId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
    }

    protected abstract boolean insertLikeIfAbsent(Long targetId, Long accountId);

    protected abstract boolean deleteLike(Long targetId, Long accountId);

    protected abstract void addLikeCount(Long targetId, long delta);

    protected abstract long countByTargetId(Long targetId);

    protected abstract List<Object[]> countByTargetIds(List<Long> targetIds);

    protected abstract Set<Long> findLikedTargetIds(Long accountId, List<Long> targetIds);
}
