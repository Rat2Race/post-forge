package dev.iamrat.source.news.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NaverNewsPropertiesTest {

    @Test
    @DisplayName("네이버 뉴스 설정 기본값을 제공한다")
    void exposesDefaults() {
        NaverNewsProperties properties = new NaverNewsProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getBaseUrl()).isEqualTo("https://openapi.naver.com");
        assertThat(properties.getClientId()).isEmpty();
        assertThat(properties.getClientSecret()).isEmpty();
        assertThat(properties.getSort()).isEqualTo("date");
        assertThat(properties.getDisplay()).isEqualTo(10);
        assertThat(properties.credentialsConfigured()).isFalse();
        assertThat(properties.isCredentialsValidForEnabledSource()).isTrue();
    }

    @Test
    @DisplayName("네이버 뉴스 설정값을 정규화한다")
    void normalizesConfiguredValues() {
        NaverNewsProperties properties = new NaverNewsProperties();

        properties.setBaseUrl("  ");
        properties.setClientId("  client-id  ");
        properties.setClientSecret("  client-secret  ");
        properties.setSort(null);
        properties.setDisplay(150);

        assertThat(properties.getBaseUrl()).isEqualTo("https://openapi.naver.com");
        assertThat(properties.getClientId()).isEqualTo("client-id");
        assertThat(properties.getClientSecret()).isEqualTo("client-secret");
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

        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");

        assertThat(properties.credentialsConfigured()).isTrue();
        assertThat(properties.isCredentialsValidForEnabledSource()).isTrue();
    }
}
