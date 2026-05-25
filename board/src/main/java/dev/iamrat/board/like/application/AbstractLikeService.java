package dev.iamrat.board.like.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLikeService {

    protected LikeResponse likeTarget(Long targetId, Long accountId) {
        validateTargetAndAccount(targetId, accountId);

        if (!existsByTargetIdAndAccountId(targetId, accountId)) {
            try {
                saveLike(targetId, accountId);
            } catch (DataIntegrityViolationException ignored) {
                // Another concurrent request inserted the row first.
            }
        }

        long likeCount = countByTargetId(targetId);
        updateLikeCount(targetId, likeCount);

        return new LikeResponse(true, likeCount);
    }

    protected LikeResponse unlikeTarget(Long targetId, Long accountId) {
        validateTargetAndAccount(targetId, accountId);

        deleteByTargetIdAndAccountId(targetId, accountId);

        long likeCount = countByTargetId(targetId);
        updateLikeCount(targetId, likeCount);

        return new LikeResponse(false, likeCount);
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

    protected abstract boolean existsByTargetIdAndAccountId(Long targetId, Long accountId);

    protected abstract void saveLike(Long targetId, Long accountId);

    protected abstract long countByTargetId(Long targetId);

    protected abstract void updateLikeCount(Long targetId, long likeCount);

    protected abstract void deleteByTargetIdAndAccountId(Long targetId, Long accountId);

    protected abstract List<Object[]> countByTargetIds(List<Long> targetIds);

    protected abstract Set<Long> findLikedTargetIds(Long accountId, List<Long> targetIds);
}
