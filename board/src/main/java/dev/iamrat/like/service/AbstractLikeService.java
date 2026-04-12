package dev.iamrat.like.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.like.dto.LikeResponse;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLikeService {

    protected LikeResponse likeTarget(Long targetId, String userId) {
        validateTargetAndUser(targetId, userId);

        if (!existsByTargetIdAndUserId(targetId, userId)) {
            try {
                saveLike(targetId, userId);
            } catch (DataIntegrityViolationException ignored) {
                // Another concurrent request inserted the row first.
            }
        }

        long likeCount = countByTargetId(targetId);
        updateLikeCount(targetId, likeCount);

        return new LikeResponse(true, likeCount);
    }

    protected LikeResponse unlikeTarget(Long targetId, String userId) {
        validateTargetAndUser(targetId, userId);

        deleteByTargetIdAndUserId(targetId, userId);

        long likeCount = countByTargetId(targetId);
        updateLikeCount(targetId, likeCount);

        return new LikeResponse(false, likeCount);
    }

    protected Map<Long, Long> getLikeCountMap(List<Long> targetIds) {
        if (targetIds == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
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

    protected Set<Long> getLikedTargetIds(List<Long> targetIds, String userId) {
        if (targetIds == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (userId == null || userId.isBlank() || targetIds.isEmpty()) {
            return Collections.emptySet();
        }

        return findLikedTargetIds(userId, targetIds);
    }

    private void validateTargetAndUser(Long targetId, String userId) {
        if (targetId == null || userId == null || userId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    protected abstract boolean existsByTargetIdAndUserId(Long targetId, String userId);

    protected abstract void saveLike(Long targetId, String userId);

    protected abstract long countByTargetId(Long targetId);

    protected abstract void updateLikeCount(Long targetId, long likeCount);

    protected abstract void deleteByTargetIdAndUserId(Long targetId, String userId);

    protected abstract List<Object[]> countByTargetIds(List<Long> targetIds);

    protected abstract Set<Long> findLikedTargetIds(String userId, List<Long> targetIds);
}
