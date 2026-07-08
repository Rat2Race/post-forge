package dev.iamrat.source.infrastructure.naver;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaverSearchMetricsTest {

    @Test
    @DisplayName("앱 시작 직후에도 네이버 fetch metric 시계열을 등록한다")
    void constructor_registersIdleMeters() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        new NaverSearchMetrics(meterRegistry);

        assertThat(meterRegistry.find("external_naver_fetch_success_total")
            .tag("api", "news")
            .counter()).isNotNull();
        assertThat(meterRegistry.find("external_naver_fetch_items_total")
            .tag("api", "shopping")
            .counter()).isNotNull();
        assertThat(meterRegistry.find("external_naver_fetch")
            .tag("api", "news")
            .tag("outcome", "success")
            .tag("status", "200")
            .timer()).isNotNull();
    }
}
