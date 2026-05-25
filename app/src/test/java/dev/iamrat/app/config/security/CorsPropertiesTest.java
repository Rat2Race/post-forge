package dev.iamrat.app.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    @DisplayName("app.cors.allowed-origins 설정을 바인딩한다")
    void bindsAllowedOrigins() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app.cors.allowed-origins[0]", "https://front.example")
            .withProperty("app.cors.allowed-origins[1]", "https://admin.example");

        Binder binder = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()));
        CorsProperties properties = binder.bind("app.cors", Bindable.of(CorsProperties.class)).get();

        assertThat(properties.getAllowedOrigins())
            .containsExactly("https://front.example", "https://admin.example");
    }
}
