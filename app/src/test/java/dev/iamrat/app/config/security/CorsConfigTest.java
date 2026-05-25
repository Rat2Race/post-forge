package dev.iamrat.app.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    @DisplayName("CORS 설정은 app 모듈 전용 properties에서 허용 origin을 읽는다")
    void corsConfigurationSource_usesAppCorsProperties() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://front.example"));
        CorsConfig config = new CorsConfig(properties);

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration corsConfiguration =
            source.getCorsConfiguration(new MockHttpServletRequest("GET", "/posts"));

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOrigins()).containsExactly("https://front.example");
        assertThat(corsConfiguration.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(corsConfiguration.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
        assertThat(corsConfiguration.getMaxAge()).isEqualTo(3600L);
    }
}
