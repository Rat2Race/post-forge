package dev.iamrat.support.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PostForgeClockConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PostForgeClockConfig.class);

    @Test
    @DisplayName("기본 시간대는 한국 시간대를 사용한다")
    void usesKoreanTimeZoneByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
        });
    }

    @Test
    @DisplayName("설정한 시간대를 Clock에 적용한다")
    void usesConfiguredTimeZone() {
        contextRunner
            .withPropertyValues("postforge.time.zone=UTC")
            .run(context -> assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("UTC")));
    }
}
