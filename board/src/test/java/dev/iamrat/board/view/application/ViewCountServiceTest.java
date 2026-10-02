package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountServiceTest {

    @Mock
    private ViewCountStore viewCountStore;

    @Mock
    private PostViewCountService postViewCountService;

    @InjectMocks
    private ViewCountService viewCountService;

    @Test
    @DisplayName("캐시에 조회수가 없으면 DB 값을 캐시에 적재하고 반환한다")
    void getViewCount_cacheMiss_loadsFromDb() {
        given(viewCountStore.findViewCountAndRefreshTtl(1L)).willReturn(Optional.empty());
        given(postViewCountService.getViewCount(1L)).willReturn(12L);

        long viewCount = viewCountService.getViewCount(1L);

        assertThat(viewCount).isEqualTo(12L);
        verify(viewCountStore).cacheViewCountIfAbsent(1L, 12L);
    }

    @Test
    @DisplayName("24시간 내 첫 조회만 조회수를 증가시키고 dirty 집합에 등록한다")
    void incrementIfNew_firstView_incrementsAndMarksDirty() {
        given(viewCountStore.markViewedIfAbsent(1L, 10L)).willReturn(true);
        given(viewCountStore.findViewCount(1L)).willReturn(Optional.of(12L));

        viewCountService.incrementIfNew(1L, 10L);

        verify(viewCountStore).incrementViewCount(1L);
        verify(viewCountStore).markDirty(1L);
        verify(postViewCountService, never()).getViewCount(anyLong());
    }

    @Test
    @DisplayName("일부만 캐시에 있으면 누락분을 DB에서 읽어 병합하고 캐시에 재적재한다")
    void getViewCounts_partialCacheMiss_mergesDbValuesAndCaches() {
        List<Long> postIds = List.of(1L, 2L);
        given(viewCountStore.findViewCounts(postIds)).willReturn(Map.of(1L, 10L));
        given(postViewCountService.findViewCounts(List.of(2L))).willReturn(Map.of(2L, 5L));

        Map<Long, Long> result = viewCountService.getViewCounts(postIds);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 10L, 2L, 5L));
        verify(viewCountStore).cacheViewCountsIfAbsent(Map.of(2L, 5L));
    }

    @Test
    @DisplayName("Redis가 응답하지 않으면 목록 조회수는 DB 값으로 돌려준다 — 게시글 조회를 막지 않는다")
    void getViewCounts_redisDown_fallsBackToDb() {
        List<Long> postIds = List.of(1L, 2L);
        given(viewCountStore.findViewCounts(postIds)).willThrow(new QueryTimeoutException("Redis command timed out"));
        given(postViewCountService.findViewCounts(postIds)).willReturn(Map.of(1L, 10L, 2L, 5L));
        willThrow(new QueryTimeoutException("Redis command timed out")).given(viewCountStore).cacheViewCountsIfAbsent(any());

        Map<Long, Long> result = viewCountService.getViewCounts(postIds);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 10L, 2L, 5L));
    }

    @Test
    @DisplayName("Redis가 응답하지 않으면 상세 조회수는 DB 값으로 돌려준다")
    void getViewCount_redisDown_fallsBackToDb() {
        given(viewCountStore.findViewCountAndRefreshTtl(1L)).willThrow(new QueryTimeoutException("Redis command timed out"));
        given(postViewCountService.getViewCount(1L)).willReturn(12L);

        assertThat(viewCountService.getViewCount(1L)).isEqualTo(12L);
    }

    @Test
    @DisplayName("Redis가 응답하지 않으면 조회수 증가는 건너뛰고 예외를 내지 않는다")
    void incrementIfNew_redisDown_skipsWithoutThrowing() {
        given(viewCountStore.markViewedIfAbsent(1L, 10L)).willThrow(new QueryTimeoutException("Redis command timed out"));

        assertThatCode(() -> viewCountService.incrementIfNew(1L, 10L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis가 응답하지 않아도 게시글 삭제 때 조회수 캐시 삭제는 실패하지 않는다")
    void deleteViewCount_redisDown_doesNotThrow() {
        willThrow(new QueryTimeoutException("Redis command timed out")).given(viewCountStore).deleteViewCount(1L);

        assertThatCode(() -> viewCountService.deleteViewCount(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis 장애로 DB에 기대는 길에서 DB 오류는 숨기지 않는다")
    void getViewCount_redisAndDbDown_propagatesDbError() {
        given(viewCountStore.findViewCountAndRefreshTtl(1L)).willThrow(new QueryTimeoutException("Redis command timed out"));
        given(postViewCountService.getViewCount(1L)).willThrow(new CannotCreateTransactionException("DB down"));

        assertThatThrownBy(() -> viewCountService.getViewCount(1L)).isInstanceOf(CannotCreateTransactionException.class);
    }
}
