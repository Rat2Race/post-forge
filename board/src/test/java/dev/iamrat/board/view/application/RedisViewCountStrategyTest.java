package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisViewCountStrategyTest {

    @Mock
    private ViewCountRedisStore viewCountStore;

    @Mock
    private PostViewCountService postViewCountService;

    private MeterRegistry meterRegistry;

    private ViewCountProperties viewCountProperties;

    private RedisViewCountStrategy viewCountStrategy;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        viewCountProperties = properties(ViewCountMode.REDIS);
        viewCountStrategy = new RedisViewCountStrategy(
            viewCountStore,
            postViewCountService,
            new ViewCountMetrics(meterRegistry, viewCountProperties)
        );
    }

    @Test
    @DisplayName("캐시에 조회수가 있으면 DB를 조회하지 않고 hit metric을 기록한다")
    void getViewCount_cacheHit_recordsHitMetric() {
        given(viewCountStore.findViewCountAndRefreshTtl(1L)).willReturn(Optional.of(12L));

        long viewCount = viewCountStrategy.getViewCount(1L);

        assertThat(viewCount).isEqualTo(12L);
        verify(postViewCountService, never()).getViewCount(anyLong());
        assertThat(counter(ViewCountMetrics.CACHE_REQUESTS, ViewCountMetrics.OPERATION_GET, ViewCountMetrics.RESULT_HIT))
            .isEqualTo(1.0);
        assertThat(timerCount(ViewCountMetrics.OPERATION_GET, ViewCountMetrics.RESULT_HIT)).isEqualTo(1);
    }

    @Test
    @DisplayName("캐시에 조회수가 없으면 DB 값을 캐시에 적재하고 반환한다")
    void getViewCount_cacheMiss_loadsFromDb() {
        given(viewCountStore.findViewCountAndRefreshTtl(1L)).willReturn(Optional.empty());
        given(postViewCountService.getViewCount(1L)).willReturn(12L);

        long viewCount = viewCountStrategy.getViewCount(1L);

        assertThat(viewCount).isEqualTo(12L);
        verify(viewCountStore).cacheViewCountIfAbsent(1L, 12L);
        assertThat(counter(ViewCountMetrics.CACHE_REQUESTS, ViewCountMetrics.OPERATION_GET, ViewCountMetrics.RESULT_MISS))
            .isEqualTo(1.0);
        assertThat(counter(
            ViewCountMetrics.DB_LOADS,
            ViewCountMetrics.OPERATION_LOAD_FROM_DB,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(1.0);
        assertThat(timerCount(ViewCountMetrics.OPERATION_LOAD_FROM_DB, ViewCountMetrics.RESULT_SUCCESS)).isEqualTo(1);
    }

    @Test
    @DisplayName("목록 조회수는 cache hit/miss와 DB fallback을 집계량으로 기록한다")
    void getViewCounts_partialCacheMiss_recordsAggregateMetrics() {
        given(viewCountStore.findViewCounts(List.of(1L, 2L, 3L)))
            .willReturn(Map.of(1L, 5L, 2L, 7L));
        given(postViewCountService.findViewCounts(List.of(3L))).willReturn(Map.of(3L, 9L));

        Map<Long, Long> viewCounts = viewCountStrategy.getViewCounts(List.of(1L, 2L, 3L));

        assertThat(viewCounts).containsEntry(1L, 5L).containsEntry(2L, 7L).containsEntry(3L, 9L);
        assertThat(counter(
            ViewCountMetrics.CACHE_REQUESTS,
            ViewCountMetrics.OPERATION_GET_MANY,
            ViewCountMetrics.RESULT_HIT
        )).isEqualTo(2.0);
        assertThat(counter(
            ViewCountMetrics.CACHE_REQUESTS,
            ViewCountMetrics.OPERATION_GET_MANY,
            ViewCountMetrics.RESULT_MISS
        )).isEqualTo(1.0);
        assertThat(counter(
            ViewCountMetrics.DB_LOADS,
            ViewCountMetrics.OPERATION_LOAD_FROM_DB,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(1.0);
    }

    @Test
    @DisplayName("목록 조회수 metric은 중복 입력도 요청 항목 기준으로 hit을 집계한다")
    void getViewCounts_duplicateCachedIds_recordsHitPerRequestedItem() {
        given(viewCountStore.findViewCounts(List.of(1L, 1L, 2L)))
            .willReturn(Map.of(1L, 5L, 2L, 7L));

        Map<Long, Long> viewCounts = viewCountStrategy.getViewCounts(List.of(1L, 1L, 2L));

        assertThat(viewCounts).containsEntry(1L, 5L).containsEntry(2L, 7L);
        verify(postViewCountService, never()).findViewCounts(any());
        assertThat(counter(
            ViewCountMetrics.CACHE_REQUESTS,
            ViewCountMetrics.OPERATION_GET_MANY,
            ViewCountMetrics.RESULT_HIT
        )).isEqualTo(3.0);
        assertThat(timerCount(ViewCountMetrics.OPERATION_GET_MANY, ViewCountMetrics.RESULT_HIT)).isEqualTo(1);
    }

    @Test
    @DisplayName("24시간 내 중복 조회는 증가를 건너뛰고 skipped metric을 기록한다")
    void incrementIfNew_duplicateView_recordsSkippedMetric() {
        given(viewCountStore.markViewedIfAbsent(1L, 10L)).willReturn(false);

        viewCountStrategy.incrementIfNew(1L, 10L);

        verify(viewCountStore, never()).incrementViewCount(1L);
        verify(viewCountStore, never()).markDirty(1L);
        assertThat(counter(
            ViewCountMetrics.OPERATIONS,
            ViewCountMetrics.OPERATION_INCREMENT,
            ViewCountMetrics.RESULT_SKIPPED
        )).isEqualTo(1.0);
    }

    @Test
    @DisplayName("24시간 내 첫 조회만 조회수를 증가시키고 dirty set에 등록한다")
    void incrementIfNew_firstView_incrementsAndMarksDirty() {
        given(viewCountStore.markViewedIfAbsent(1L, 10L)).willReturn(true);
        given(viewCountStore.findViewCount(1L)).willReturn(Optional.of(12L));

        viewCountStrategy.incrementIfNew(1L, 10L);

        verify(viewCountStore).incrementViewCount(1L);
        verify(viewCountStore).markDirty(1L);
        verify(postViewCountService, never()).getViewCount(anyLong());
        assertThat(counter(
            ViewCountMetrics.CACHE_REQUESTS,
            ViewCountMetrics.OPERATION_INCREMENT,
            ViewCountMetrics.RESULT_HIT
        )).isEqualTo(1.0);
        assertThat(counter(
            ViewCountMetrics.OPERATIONS,
            ViewCountMetrics.OPERATION_INCREMENT,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(1.0);
        assertThat(counter(
            ViewCountMetrics.OPERATIONS,
            ViewCountMetrics.OPERATION_MARK_DIRTY,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(1.0);
        assertThat(timerCount(ViewCountMetrics.OPERATION_INCREMENT, ViewCountMetrics.RESULT_SUCCESS)).isEqualTo(1);
    }

    @Test
    @DisplayName("첫 조회 증가 중 캐시에 값이 없으면 DB load metric을 기록한다")
    void incrementIfNew_cacheMiss_loadsFromDbAndRecordsMetrics() {
        given(viewCountStore.markViewedIfAbsent(1L, 10L)).willReturn(true);
        given(viewCountStore.findViewCount(1L)).willReturn(Optional.empty());
        given(postViewCountService.getViewCount(1L)).willReturn(12L);

        viewCountStrategy.incrementIfNew(1L, 10L);

        verify(viewCountStore).cacheViewCountIfAbsent(1L, 12L);
        verify(viewCountStore).incrementViewCount(1L);
        verify(viewCountStore).markDirty(1L);
        assertThat(counter(
            ViewCountMetrics.CACHE_REQUESTS,
            ViewCountMetrics.OPERATION_INCREMENT,
            ViewCountMetrics.RESULT_MISS
        )).isEqualTo(1.0);
        assertThat(counter(
            ViewCountMetrics.DB_LOADS,
            ViewCountMetrics.OPERATION_LOAD_FROM_DB,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(1.0);
    }

    @Test
    @DisplayName("조회수 저장소 실패는 예외를 유지하고 failure metric을 기록한다")
    void getViewCount_storeFailure_recordsFailureMetricAndRethrows() {
        given(viewCountStore.findViewCountAndRefreshTtl(1L))
            .willThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> viewCountStrategy.getViewCount(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("redis down");

        assertThat(counter(
            ViewCountMetrics.CACHE_REQUESTS,
            ViewCountMetrics.OPERATION_GET,
            ViewCountMetrics.RESULT_FAILURE
        )).isEqualTo(1.0);
        assertThat(timerCount(ViewCountMetrics.OPERATION_GET, ViewCountMetrics.RESULT_FAILURE)).isEqualTo(1);
    }

    private double counter(String metricName, String operation, String result) {
        return counter(metricName, operation, result, ViewCountMetrics.CACHE_STATE_REDIS_ENABLED);
    }

    private double counter(String metricName, String operation, String result, String cacheState) {
        return meterRegistry.get(metricName)
            .tag(ViewCountMetrics.TAG_ENDPOINT, ViewCountMetrics.ENDPOINT)
            .tag(ViewCountMetrics.TAG_CACHE_STATE, cacheState)
            .tag(ViewCountMetrics.TAG_OPERATION, operation)
            .tag(ViewCountMetrics.TAG_RESULT, result)
            .counter()
            .count();
    }

    private long timerCount(String operation, String result) {
        return timerCount(operation, result, ViewCountMetrics.CACHE_STATE_REDIS_ENABLED);
    }

    private long timerCount(String operation, String result, String cacheState) {
        return meterRegistry.get(ViewCountMetrics.OPERATION_DURATION)
            .tag(ViewCountMetrics.TAG_ENDPOINT, ViewCountMetrics.ENDPOINT)
            .tag(ViewCountMetrics.TAG_CACHE_STATE, cacheState)
            .tag(ViewCountMetrics.TAG_OPERATION, operation)
            .tag(ViewCountMetrics.TAG_RESULT, result)
            .timer()
            .count();
    }

    private ViewCountProperties properties(ViewCountMode mode) {
        ViewCountProperties properties = new ViewCountProperties();
        properties.setMode(mode);
        return properties;
    }
}
