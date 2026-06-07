package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "postforge.view-count", name = "mode", havingValue = "redis", matchIfMissing = true)
public class RedisViewCountStrategy implements ViewCountStrategy {

    private final ViewCountRedisStore viewCountStore;
    private final PostViewCountService postViewCountService;
    private final ViewCountMetrics viewCountMetrics;

    @Override
    public void incrementIfNew(Long postId, Long accountId) {
        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            if (!markViewedIfAbsent(postId, accountId)) {
                viewCountMetrics.recordOperation(
                    ViewCountMetrics.OPERATION_INCREMENT,
                    ViewCountMetrics.RESULT_SKIPPED
                );
                durationResult = ViewCountMetrics.RESULT_SKIPPED;
                return;
            }
            if (findViewCountForIncrement(postId).isEmpty()) {
                loadFromDb(postId);
            }
            incrementViewCount(postId);
            markDirty(postId);
            durationResult = ViewCountMetrics.RESULT_SUCCESS;
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_INCREMENT, durationResult);
        }
    }

    @Override
    public long getViewCount(Long postId) {
        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            Optional<Long> cachedViewCount = findViewCountAndRefreshTtl(postId);
            if (cachedViewCount.isPresent()) {
                durationResult = ViewCountMetrics.RESULT_HIT;
                return cachedViewCount.get();
            }
            durationResult = ViewCountMetrics.RESULT_MISS;
            return loadFromDb(postId);
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_GET, durationResult);
        }
    }

    @Override
    public Map<Long, Long> getViewCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            Map<Long, Long> result = new HashMap<>(findViewCounts(postIds));
            long hitCount = postIds.stream()
                .filter(result::containsKey)
                .count();
            long missCount = postIds.size() - hitCount;
            viewCountMetrics.recordCacheRequest(
                ViewCountMetrics.OPERATION_GET_MANY,
                ViewCountMetrics.RESULT_HIT,
                hitCount
            );
            viewCountMetrics.recordCacheRequest(
                ViewCountMetrics.OPERATION_GET_MANY,
                ViewCountMetrics.RESULT_MISS,
                missCount
            );

            List<Long> missedIds = postIds.stream()
                .filter(postId -> !result.containsKey(postId))
                .toList();

            if (!missedIds.isEmpty()) {
                result.putAll(loadFromDb(missedIds));
            }
            durationResult = missedIds.isEmpty() ? ViewCountMetrics.RESULT_HIT : ViewCountMetrics.RESULT_MISS;
            return result;
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_GET_MANY, durationResult);
        }
    }

    @Override
    public void deleteViewCount(Long postId) {
        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            viewCountStore.deleteViewCount(postId);
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_DELETE, ViewCountMetrics.RESULT_SUCCESS);
            durationResult = ViewCountMetrics.RESULT_SUCCESS;
        } catch (RuntimeException e) {
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_DELETE, ViewCountMetrics.RESULT_FAILURE);
            throw e;
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_DELETE, durationResult);
        }
    }

    private long loadFromDb(Long postId) {
        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            Long views = postViewCountService.getViewCount(postId);
            viewCountStore.cacheViewCountIfAbsent(postId, views);
            viewCountMetrics.recordDbLoad(
                ViewCountMetrics.OPERATION_LOAD_FROM_DB,
                ViewCountMetrics.RESULT_SUCCESS
            );
            durationResult = ViewCountMetrics.RESULT_SUCCESS;
            return views;
        } catch (RuntimeException e) {
            viewCountMetrics.recordDbLoad(
                ViewCountMetrics.OPERATION_LOAD_FROM_DB,
                ViewCountMetrics.RESULT_FAILURE
            );
            throw e;
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_LOAD_FROM_DB, durationResult);
        }
    }

    private Map<Long, Long> loadFromDb(List<Long> postIds) {
        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            Map<Long, Long> result = postViewCountService.findViewCounts(postIds);

            if (!result.isEmpty()) {
                viewCountStore.cacheViewCountsIfAbsent(result);
            }
            viewCountMetrics.recordDbLoad(
                ViewCountMetrics.OPERATION_LOAD_FROM_DB,
                ViewCountMetrics.RESULT_SUCCESS,
                postIds.size()
            );
            durationResult = ViewCountMetrics.RESULT_SUCCESS;
            return result;
        } catch (RuntimeException e) {
            viewCountMetrics.recordDbLoad(
                ViewCountMetrics.OPERATION_LOAD_FROM_DB,
                ViewCountMetrics.RESULT_FAILURE,
                postIds.size()
            );
            throw e;
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_LOAD_FROM_DB, durationResult);
        }
    }

    private boolean markViewedIfAbsent(Long postId, Long accountId) {
        try {
            return viewCountStore.markViewedIfAbsent(postId, accountId);
        } catch (RuntimeException e) {
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_INCREMENT, ViewCountMetrics.RESULT_FAILURE);
            throw e;
        }
    }

    private Optional<Long> findViewCountAndRefreshTtl(Long postId) {
        try {
            Optional<Long> viewCount = viewCountStore.findViewCountAndRefreshTtl(postId);
            viewCountMetrics.recordCacheRequest(
                ViewCountMetrics.OPERATION_GET,
                viewCount.isPresent() ? ViewCountMetrics.RESULT_HIT : ViewCountMetrics.RESULT_MISS
            );
            return viewCount;
        } catch (RuntimeException e) {
            viewCountMetrics.recordCacheRequest(ViewCountMetrics.OPERATION_GET, ViewCountMetrics.RESULT_FAILURE);
            throw e;
        }
    }

    private Map<Long, Long> findViewCounts(List<Long> postIds) {
        try {
            return viewCountStore.findViewCounts(postIds);
        } catch (RuntimeException e) {
            viewCountMetrics.recordCacheRequest(
                ViewCountMetrics.OPERATION_GET_MANY,
                ViewCountMetrics.RESULT_FAILURE,
                postIds.size()
            );
            throw e;
        }
    }

    private Optional<Long> findViewCountForIncrement(Long postId) {
        try {
            Optional<Long> viewCount = viewCountStore.findViewCount(postId);
            viewCountMetrics.recordCacheRequest(
                ViewCountMetrics.OPERATION_INCREMENT,
                viewCount.isPresent() ? ViewCountMetrics.RESULT_HIT : ViewCountMetrics.RESULT_MISS
            );
            return viewCount;
        } catch (RuntimeException e) {
            viewCountMetrics.recordCacheRequest(ViewCountMetrics.OPERATION_INCREMENT, ViewCountMetrics.RESULT_FAILURE);
            throw e;
        }
    }

    private void incrementViewCount(Long postId) {
        try {
            viewCountStore.incrementViewCount(postId);
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_INCREMENT, ViewCountMetrics.RESULT_SUCCESS);
        } catch (RuntimeException e) {
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_INCREMENT, ViewCountMetrics.RESULT_FAILURE);
            throw e;
        }
    }

    private void markDirty(Long postId) {
        try {
            viewCountStore.markDirty(postId);
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_MARK_DIRTY, ViewCountMetrics.RESULT_SUCCESS);
        } catch (RuntimeException e) {
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_MARK_DIRTY, ViewCountMetrics.RESULT_FAILURE);
            throw e;
        }
    }
}
