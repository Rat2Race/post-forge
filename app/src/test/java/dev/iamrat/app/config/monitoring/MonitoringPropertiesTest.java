package dev.iamrat.app.config.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringPropertiesTest {

    @Test
    @DisplayName("monitoring prefix로 모니터링 credential 설정을 바인딩한다")
    void monitoringProperties_usesMonitoringPrefix() {
        ConfigurationProperties annotation =
            MonitoringProperties.class.getAnnotation(ConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("monitoring");
    }
}
