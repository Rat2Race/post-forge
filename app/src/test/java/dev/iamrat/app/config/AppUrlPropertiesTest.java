package dev.iamrat.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 도메인 환경변수는 기본값을 두지 않는다.
 * 값이 비면 앱이 뜨긴 뜨고 URL만 조용히 틀리는 쪽이 가장 위험하므로, 기동 자체를 실패시킨다.
 */
@Tag("unit")
class AppUrlPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
            ConfigurationPropertiesAutoConfiguration.class,
            ValidationAutoConfiguration.class))
        .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("도메인이 비어 있으면 기동에 실패한다")
    void blankFrontendFailsStartup() {
        runner.withPropertyValues("app.url.frontend=", "app.url.api=https://api.example")
            .run(context -> assertThat(context)
                .as("빈 값으로 부팅되면 배포 서버가 잘못된 URL로 조용히 동작한다")
                .hasFailed());
    }

    @Test
    @DisplayName("스킴이 없으면 기동에 실패한다")
    void schemelessApiFailsStartup() {
        runner.withPropertyValues("app.url.frontend=https://front.example", "app.url.api=api.example")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("끝에 슬래시가 있으면 기동에 실패한다")
    void trailingSlashFailsStartup() {
        runner.withPropertyValues("app.url.frontend=https://front.example/", "app.url.api=https://api.example")
            .run(context -> assertThat(context)
                .as("경로를 뒤에 붙이므로 슬래시가 겹치면 provider 등록값과 어긋난다")
                .hasFailed());
    }

    @Test
    @DisplayName("정상 도메인은 그대로 바인딩된다")
    void validUrlsBind() {
        runner.withPropertyValues("app.url.frontend=https://front.example", "app.url.api=https://api.example")
            .run(context -> {
                assertThat(context).hasNotFailed();
                AppUrlProperties properties = context.getBean(AppUrlProperties.class);
                assertThat(properties.getFrontend()).isEqualTo("https://front.example");
                assertThat(properties.getApi()).isEqualTo("https://api.example");
            });
    }

    @Configuration
    @EnableConfigurationProperties(AppUrlProperties.class)
    static class TestConfig {
    }
}
