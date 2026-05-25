package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final ViewCountStore viewCountStore;
    private final PostViewCountService postViewCountService;

    public void incrementIfNew(Long postId, Long accountId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        if (accountId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);

        if (viewCountStore.markViewedIfAbsent(postId, accountId)) {
            if (viewCountStore.findViewCount(postId).isEmpty()) {
                loadFromDb(postId);
            }
            viewCountStore.incrementViewCount(postId);
            viewCountStore.markDirty(postId);
        }
    }

    public long getViewCount(Long postId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        return viewCountStore.findViewCountAndRefreshTtl(postId)
            .orElseGet(() -> loadFromDb(postId));
    }

    public Map<Long, Long> getViewCounts(List<Long> postIds) {
        if (postIds == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>(viewCountStore.findViewCounts(postIds));
        List<Long> missedIds = postIds.stream()
            .filter(postId -> !result.containsKey(postId))
            .toList();

        if (!missedIds.isEmpty()) {
            result.putAll(loadFromDb(missedIds));
        }

        return result;
    }

    public void deleteViewCount(Long postId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        viewCountStore.deleteViewCount(postId);
    }

    private long loadFromDb(Long postId) {
        Long views = postViewCountService.getViewCount(postId);
        
        viewCountStore.cacheViewCountIfAbsent(postId, views);
        
        return views;
    }

    private Map<Long, Long> loadFromDb(List<Long> postIds) {
        Map<Long, Long> result = postViewCountService.findViewCounts(postIds);

        if (!result.isEmpty()) {
            viewCountStore.cacheViewCountsIfAbsent(result);
        }

        return result;
    }
}
