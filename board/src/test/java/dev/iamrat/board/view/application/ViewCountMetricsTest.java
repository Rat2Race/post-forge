package dev.iamrat.board.view.application;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewCountMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private ViewCountProperties viewCountProperties;
    private ViewCountMetrics viewCountMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        viewCountProperties = properties(ViewCountMode.REDIS);
        viewCountMetrics = new ViewCountMetrics(meterRegistry, viewCountProperties);
    }

    @Test
    @DisplayName("cache 요청 metric은 낮은 cardinality tag만 사용한다")
    void recordCacheRequest_usesBoundedTags() {
        viewCountMetrics.recordCacheRequest(
            ViewCountMetrics.OPERATION_GET,
            ViewCountMetrics.RESULT_HIT
        );

        assertThat(counter(
            ViewCountMetrics.CACHE_REQUESTS,
            ViewCountMetrics.OPERATION_GET,
            ViewCountMetrics.RESULT_HIT
        )).isEqualTo(1.0);
        assertTagHygiene();
    }

    @Test
    @DisplayName("batch miss metric은 게시글별 series가 아니라 집계량을 증가시킨다")
    void recordCacheRequest_withAmount_incrementsSingleSeries() {
        viewCountMetrics.recordCacheRequest(
            ViewCountMetrics.OPERATION_GET_MANY,
            ViewCountMetrics.RESULT_MISS,
            3
        );

        assertThat(counter(
            ViewCountMetrics.CACHE_REQUESTS,
            ViewCountMetrics.OPERATION_GET_MANY,
            ViewCountMetrics.RESULT_MISS
        )).isEqualTo(3.0);
        assertThat(meterRegistry.getMeters()).hasSize(1);
    }

    @Test
    @DisplayName("DB load와 operation metric을 같은 label 계약으로 기록한다")
    void recordDbLoadAndOperation_useSameTagContract() {
        viewCountMetrics.recordDbLoad(
            ViewCountMetrics.OPERATION_LOAD_FROM_DB,
            ViewCountMetrics.RESULT_SUCCESS
        );
        viewCountMetrics.recordOperation(
            ViewCountMetrics.OPERATION_INCREMENT,
            ViewCountMetrics.RESULT_SKIPPED
        );

        assertThat(counter(
            ViewCountMetrics.DB_LOADS,
            ViewCountMetrics.OPERATION_LOAD_FROM_DB,
            ViewCountMetrics.RESULT_SUCCESS
        )).isEqualTo(1.0);
        assertThat(counter(
            ViewCountMetrics.OPERATIONS,
            ViewCountMetrics.OPERATION_INCREMENT,
            ViewCountMetrics.RESULT_SKIPPED
        )).isEqualTo(1.0);
        assertTagHygiene();
    }

    @Test
    @DisplayName("duration timer도 bounded label만 사용한다")
    void recordDuration_usesBoundedTags() {
        viewCountMetrics.recordDuration(
            ViewCountMetrics.OPERATION_GET,
            ViewCountMetrics.RESULT_HIT,
            Duration.ofMillis(7)
        );

        assertThat(meterRegistry.get(ViewCountMetrics.OPERATION_DURATION)
            .tag(ViewCountMetrics.TAG_ENDPOINT, ViewCountMetrics.ENDPOINT)
            .tag(ViewCountMetrics.TAG_CACHE_STATE, ViewCountMetrics.CACHE_STATE_REDIS_ENABLED)
            .tag(ViewCountMetrics.TAG_OPERATION, ViewCountMetrics.OPERATION_GET)
            .tag(ViewCountMetrics.TAG_RESULT, ViewCountMetrics.RESULT_HIT)
            .timer()
            .count()).isEqualTo(1);
        assertTagHygiene();
    }

    @Test
    @DisplayName("SQL mode에서는 cache_state를 sql_only로 기록한다")
    void recordDbLoad_sqlMode_usesSqlOnlyCacheState() {
        viewCountProperties.setMode(ViewCountMode.SQL);

        viewCountMetrics.recordDbLoad(
            ViewCountMetrics.OPERATION_GET,
            ViewCountMetrics.RESULT_SUCCESS
        );

        assertThat(meterRegistry.get(ViewCountMetrics.DB_LOADS)
            .tag(ViewCountMetrics.TAG_ENDPOINT, ViewCountMetrics.ENDPOINT)
            .tag(ViewCountMetrics.TAG_CACHE_STATE, ViewCountMetrics.CACHE_STATE_SQL_ONLY)
            .tag(ViewCountMetrics.TAG_OPERATION, ViewCountMetrics.OPERATION_GET)
            .tag(ViewCountMetrics.TAG_RESULT, ViewCountMetrics.RESULT_SUCCESS)
            .counter()
            .count()).isEqualTo(1.0);
        assertTagHygiene();
    }

    private double counter(String metricName, String operation, String result) {
        return meterRegistry.get(metricName)
            .tag(ViewCountMetrics.TAG_ENDPOINT, ViewCountMetrics.ENDPOINT)
            .tag(ViewCountMetrics.TAG_CACHE_STATE, ViewCountMetrics.CACHE_STATE_REDIS_ENABLED)
            .tag(ViewCountMetrics.TAG_OPERATION, operation)
            .tag(ViewCountMetrics.TAG_RESULT, result)
            .counter()
            .count();
    }

    private void assertTagHygiene() {
        Set<String> allowedTagKeys = Set.of(
            ViewCountMetrics.TAG_ENDPOINT,
            ViewCountMetrics.TAG_CACHE_STATE,
            ViewCountMetrics.TAG_OPERATION,
            ViewCountMetrics.TAG_RESULT
        );
        Set<String> forbiddenFragments = Set.of(
            "postId",
            "accountId",
            "username",
            "email",
            "token",
            "RUN_GROUP",
            "/posts/"
        );

        for (Meter meter : meterRegistry.getMeters()) {
            assertThat(meter.getId().getName()).startsWith("postforge_view_count_");
            for (var tag : meter.getId().getTags()) {
                assertThat(allowedTagKeys).contains(tag.getKey());
                for (String forbiddenFragment : forbiddenFragments) {
                    assertThat(tag.getKey()).doesNotContain(forbiddenFragment);
                    assertThat(tag.getValue()).doesNotContain(forbiddenFragment);
                }
            }
        }
    }

    private ViewCountProperties properties(ViewCountMode mode) {
        ViewCountProperties properties = new ViewCountProperties();
        properties.setMode(mode);
        return properties;
    }
}
