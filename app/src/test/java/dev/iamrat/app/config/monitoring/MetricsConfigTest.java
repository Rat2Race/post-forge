package dev.iamrat.app.config.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsConfigTest {

    @Test
    @DisplayName("MetricsConfig에서 monitoring properties를 등록한다")
    void metricsConfig_enablesMonitoringProperties() {
        EnableConfigurationProperties annotation =
            MetricsConfig.class.getAnnotation(EnableConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(MonitoringProperties.class);
    }
}
