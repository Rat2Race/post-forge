package dev.iamrat.source.product.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NaverShoppingPropertiesTest {

    @Test
    @DisplayName("네이버 쇼핑 설정 기본값을 제공한다")
    void exposesDefaults() {
        NaverShoppingProperties properties = new NaverShoppingProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getBaseUrl()).isEqualTo("https://openapi.naver.com");
        assertThat(properties.getClientId()).isEmpty();
        assertThat(properties.getClientSecret()).isEmpty();
        assertThat(properties.getSort()).isEqualTo("sim");
        assertThat(properties.getExclude()).isEqualTo("used:rental:cbshop");
        assertThat(properties.credentialsConfigured()).isFalse();
        assertThat(properties.isCredentialsValidForEnabledSource()).isTrue();
    }

    @Test
    @DisplayName("네이버 쇼핑 설정값을 정규화한다")
    void normalizesConfiguredValues() {
        NaverShoppingProperties properties = new NaverShoppingProperties();

        properties.setBaseUrl("  ");
        properties.setClientId("  client-id  ");
        properties.setClientSecret("  client-secret  ");
        properties.setSort(null);
        properties.setExclude("  ");

        assertThat(properties.getBaseUrl()).isEqualTo("https://openapi.naver.com");
        assertThat(properties.getClientId()).isEqualTo("client-id");
        assertThat(properties.getClientSecret()).isEqualTo("client-secret");
        assertThat(properties.getSort()).isEqualTo("sim");
        assertThat(properties.getExclude()).isEqualTo("used:rental:cbshop");
    }

    @Test
    @DisplayName("네이버 쇼핑 연동이 활성화된 경우에만 인증값을 검증한다")
    void validatesCredentialsOnlyWhenEnabled() {
        NaverShoppingProperties properties = new NaverShoppingProperties();

        properties.setEnabled(true);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isCredentialsValidForEnabledSource()).isFalse();

        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");

        assertThat(properties.credentialsConfigured()).isTrue();
        assertThat(properties.isCredentialsValidForEnabledSource()).isTrue();
    }
}
