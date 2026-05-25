package dev.iamrat.collector.source.infrastructure.external.naver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class NaverNewsPropertiesTest {

    @Test
    @DisplayName("키워드 설정은 공백과 중복을 제거해 순서대로 반환한다")
    void keywords_trimsBlanksAndDuplicates() {
        NaverNewsProperties properties = new NaverNewsProperties();
        properties.setKeywords("테크, ,정책,테크,,");

        assertThat(properties.keywords()).containsExactly("테크", "정책");
    }

    @Test
    @DisplayName("클라이언트 인증값과 키워드가 모두 있어야 configured 상태다")
    void isConfigured_requiresCredentialsAndKeywords() {
        NaverNewsProperties properties = new NaverNewsProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setKeywords("테크");

        assertThat(properties.isConfigured()).isTrue();

        properties.setClientSecret("");

        assertThat(properties.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("collector.naver-news prefix로 외부 API 설정을 바인딩한다")
    void naverNewsProperties_usesCollectorNaverNewsPrefix() {
        ConfigurationProperties annotation =
            NaverNewsProperties.class.getAnnotation(ConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("collector.naver-news");
    }
}
