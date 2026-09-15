package dev.iamrat.board.view.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ViewCountStore {

    boolean markViewedIfAbsent(Long postId, Long accountId);

    Optional<Long> findViewCount(Long postId);

    Optional<Long> findViewCountAndRefreshTtl(Long postId);

    Map<Long, Long> findViewCounts(List<Long> postIds);

    void incrementViewCount(Long postId);

    void cacheViewCountIfAbsent(Long postId, Long views);

    void cacheViewCountsIfAbsent(Map<Long, Long> viewCounts);

    void markDirty(Long postId);

    void deleteViewCount(Long postId);

    Optional<String> claimDirtyIdsForProcessing();

    Set<String> findDirtyIds(String dirtyKey);

    void deleteDirtyIds(String dirtyKey);

    void removeProcessedDirtyIds(String processingKey, Set<String> processedDirtyIds);
}
