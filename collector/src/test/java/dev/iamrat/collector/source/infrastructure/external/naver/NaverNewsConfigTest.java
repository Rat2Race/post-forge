package dev.iamrat.collector.source.infrastructure.external.naver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class NaverNewsConfigTest {

    @Test
    @DisplayName("외부 API 설정 properties를 활성화한다")
    void naverNewsConfig_enablesNaverNewsProperties() {
        EnableConfigurationProperties annotation =
            NaverNewsConfig.class.getAnnotation(EnableConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(NaverNewsProperties.class);
    }

    @Test
    @DisplayName("collector.naver-news prefix 바인딩은 properties가 담당한다")
    void naverNewsConfig_doesNotBindPropertiesDirectly() {
        assertThat(NaverNewsConfig.class.getAnnotation(ConfigurationProperties.class)).isNull();
    }
}
