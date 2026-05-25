package dev.iamrat.app.config.scheduling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingConfigTest {

    @Test
    @DisplayName("app scheduling config에서 스케줄링을 활성화한다")
    void schedulingConfig_enablesScheduling() {
        assertThat(MergedAnnotations.from(SchedulingConfig.class)
            .isPresent(EnableScheduling.class))
            .isTrue();
    }
}
