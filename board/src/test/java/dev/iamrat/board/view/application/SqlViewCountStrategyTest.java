package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqlViewCountStrategyTest {

    @Mock
    private PostViewCountService postViewCountService;

    private MeterRegistry meterRegistry;
    private SqlViewCountStrategy strategy;

    @BeforeEach
    void setUp() {
        ViewCountProperties properties = new ViewCountProperties();
        properties.setMode(ViewCountMode.SQL);
        meterRegistry = new SimpleMeterRegistry();
        strategy = new SqlViewCountStrategy(
            postViewCountService,
            new ViewCountMetrics(meterRegistry, properties)
        );
    }

    @Test
    @DisplayName("SQL strategy는 단건 조회를 DB load로 기록한다")
    void getViewCount_recordsDbLoad() {
        given(postViewCountService.getViewCount(1L)).willReturn(12L);

        long viewCount = strategy.getViewCount(1L);

        assertThat(viewCount).isEqualTo(12L);
        assertThat(counter(
            ViewCountMetrics.DB_LOADS,
            ViewCountMetrics.OPERATION_GET,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(1.0);
        assertThat(meterRegistry.find(ViewCountMetrics.CACHE_REQUESTS).meters()).isEmpty();
        assertThat(timerCount(ViewCountMetrics.OPERATION_GET, ViewCountMetrics.RESULT_SUCCESS)).isEqualTo(1);
    }

    @Test
    @DisplayName("SQL strategy는 목록 조회를 DB load 집계량으로 기록한다")
    void getViewCounts_recordsDbLoadAmount() {
        given(postViewCountService.findViewCounts(List.of(1L, 2L))).willReturn(Map.of(1L, 12L, 2L, 7L));

        Map<Long, Long> viewCounts = strategy.getViewCounts(List.of(1L, 2L));

        assertThat(viewCounts).isEqualTo(Map.of(1L, 12L, 2L, 7L));
        assertThat(counter(
            ViewCountMetrics.DB_LOADS,
            ViewCountMetrics.OPERATION_GET_MANY,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(2.0);
    }

    @Test
    @DisplayName("SQL strategy는 조회수를 DB에서 직접 증가시킨다")
    void incrementIfNew_delegatesDbIncrement() {
        strategy.incrementIfNew(1L, 10L);

        verify(postViewCountService).incrementViewCount(1L);
        assertThat(counter(
            ViewCountMetrics.OPERATIONS,
            ViewCountMetrics.OPERATION_INCREMENT,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(1.0);
    }

    @Test
    @DisplayName("SQL strategy 실패 metric은 예외를 유지한다")
    void getViewCount_failureMetricRethrows() {
        given(postViewCountService.getViewCount(1L)).willThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> strategy.getViewCount(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("db down");
        assertThat(counter(
            ViewCountMetrics.DB_LOADS,
            ViewCountMetrics.OPERATION_GET,
            ViewCountMetrics.RESULT_FAILURE
        )).isEqualTo(1.0);
    }

    private double counter(String metricName, String operation, String result) {
        return meterRegistry.get(metricName)
            .tag(ViewCountMetrics.TAG_ENDPOINT, ViewCountMetrics.ENDPOINT)
            .tag(ViewCountMetrics.TAG_CACHE_STATE, ViewCountMetrics.CACHE_STATE_SQL_ONLY)
            .tag(ViewCountMetrics.TAG_OPERATION, operation)
            .tag(ViewCountMetrics.TAG_RESULT, result)
            .counter()
            .count();
    }

    private long timerCount(String operation, String result) {
        return meterRegistry.get(ViewCountMetrics.OPERATION_DURATION)
            .tag(ViewCountMetrics.TAG_ENDPOINT, ViewCountMetrics.ENDPOINT)
            .tag(ViewCountMetrics.TAG_CACHE_STATE, ViewCountMetrics.CACHE_STATE_SQL_ONLY)
            .tag(ViewCountMetrics.TAG_OPERATION, operation)
            .tag(ViewCountMetrics.TAG_RESULT, result)
            .timer()
            .count();
    }
}
