package dev.iamrat.source.news.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NaverNewsPropertiesTest {

    @Test
    @DisplayName("네이버 뉴스 설정 기본값을 제공한다")
    void exposesDefaults() {
        NaverNewsProperties properties = new NaverNewsProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getBaseUrl()).isEqualTo("https://naverapihub.apigw.ntruss.com");
        assertThat(properties.getApiKeyId()).isEmpty();
        assertThat(properties.getApiKey()).isEmpty();
        assertThat(properties.getSort()).isEqualTo("date");
        assertThat(properties.getDisplay()).isEqualTo(10);
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.credentialsConfigured()).isFalse();
        assertThat(properties.isCredentialsValidForEnabledSource()).isTrue();
    }

    @Test
    @DisplayName("네이버 뉴스 timeout 설정은 0 이하와 null을 기본값으로 되돌린다")
    void normalizesTimeouts() {
        NaverNewsProperties properties = new NaverNewsProperties();

        properties.setConnectTimeout(Duration.ofSeconds(5));
        properties.setReadTimeout(Duration.ofSeconds(20));

        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(20));

        properties.setConnectTimeout(Duration.ofSeconds(-1));
        properties.setReadTimeout(Duration.ZERO);

        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("네이버 뉴스 설정값을 정규화한다")
    void normalizesConfiguredValues() {
        NaverNewsProperties properties = new NaverNewsProperties();

        properties.setBaseUrl("  ");
        properties.setApiKeyId("  api-key-id  ");
        properties.setApiKey("  api-key  ");
        properties.setSort(null);
        properties.setDisplay(150);

        assertThat(properties.getBaseUrl()).isEqualTo("https://naverapihub.apigw.ntruss.com");
        assertThat(properties.getApiKeyId()).isEqualTo("api-key-id");
        assertThat(properties.getApiKey()).isEqualTo("api-key");
        assertThat(properties.getSort()).isEqualTo("date");
        assertThat(properties.getDisplay()).isEqualTo(100);

        properties.setDisplay(0);

        assertThat(properties.getDisplay()).isEqualTo(10);
    }

    @Test
    @DisplayName("네이버 뉴스 연동이 활성화된 경우에만 인증값을 검증한다")
    void validatesCredentialsOnlyWhenEnabled() {
        NaverNewsProperties properties = new NaverNewsProperties();

        properties.setEnabled(true);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isCredentialsValidForEnabledSource()).isFalse();

        properties.setApiKeyId("api-key-id");
        properties.setApiKey("api-key");

        assertThat(properties.credentialsConfigured()).isTrue();
        assertThat(properties.isCredentialsValidForEnabledSource()).isTrue();
    }
}
