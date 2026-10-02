package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 조회수는 부가 정보라 Redis가 응답하지 않아도 게시글 조회를 막지 않는다(fail-open).
 * 읽기는 DB 값(마지막 동기화 기준, 최대 5분 늦음)으로 돌려주고, 증가·캐시 적재·삭제는 건너뛴다.
 * Redis 호출만 감싼다. DB에 기대는 길의 DB 오류는 숨기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final ViewCountStore viewCountStore;
    private final PostViewCountService postViewCountService;

    public void incrementIfNew(Long postId, Long accountId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        if (accountId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);

        boolean firstView;
        try {
            firstView = viewCountStore.markViewedIfAbsent(postId, accountId);
        } catch (DataAccessException e) {
            log.warn("조회수 증가 건너뜀(Redis 장애): postId={}", postId);
            return;
        }
        if (!firstView) {
            return;
        }
        try {
            if (viewCountStore.findViewCount(postId).isEmpty()) {
                cacheQuietly(postId, postViewCountService.getViewCount(postId));
            }
            viewCountStore.incrementViewCount(postId);
            viewCountStore.markDirty(postId);
        } catch (DataAccessException e) {
            log.warn("조회수 증가 건너뜀(Redis 장애): postId={}", postId);
        }
    }

    public long getViewCount(Long postId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        Optional<Long> cached;
        try {
            cached = viewCountStore.findViewCountAndRefreshTtl(postId);
        } catch (DataAccessException e) {
            log.warn("조회수를 DB 값으로 대신함(Redis 장애): postId={}", postId);
            return postViewCountService.getViewCount(postId);
        }
        return cached.orElseGet(() -> loadFromDb(postId));
    }

    public Map<Long, Long> getViewCounts(List<Long> postIds) {
        if (postIds == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result;
        try {
            result = new HashMap<>(viewCountStore.findViewCounts(postIds));
        } catch (DataAccessException e) {
            log.warn("조회수를 DB 값으로 대신함(Redis 장애): {}건", postIds.size());
            result = new HashMap<>();
        }
        Map<Long, Long> cached = result;
        List<Long> missedIds = postIds.stream()
            .filter(postId -> !cached.containsKey(postId))
            .toList();

        if (!missedIds.isEmpty()) {
            result.putAll(loadFromDb(missedIds));
        }

        return result;
    }

    public void deleteViewCount(Long postId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        try {
            viewCountStore.deleteViewCount(postId);
        } catch (DataAccessException e) {
            // 남은 캐시 키는 TTL로 사라지고, 동기화는 없는 게시글을 갱신하지 않는다.
            log.warn("조회수 캐시 삭제 건너뜀(Redis 장애): postId={}", postId);
        }
    }

    private long loadFromDb(Long postId) {
        Long views = postViewCountService.getViewCount(postId);
        cacheQuietly(postId, views);
        return views;
    }

    private Map<Long, Long> loadFromDb(List<Long> postIds) {
        Map<Long, Long> result = postViewCountService.findViewCounts(postIds);

        if (!result.isEmpty()) {
            try {
                viewCountStore.cacheViewCountsIfAbsent(result);
            } catch (DataAccessException e) {
                log.warn("조회수 캐시 적재 건너뜀(Redis 장애): {}건", result.size());
            }
        }

        return result;
    }

    private void cacheQuietly(Long postId, Long views) {
        try {
            viewCountStore.cacheViewCountIfAbsent(postId, views);
        } catch (DataAccessException e) {
            log.warn("조회수 캐시 적재 건너뜀(Redis 장애): postId={}", postId);
        }
    }
}
