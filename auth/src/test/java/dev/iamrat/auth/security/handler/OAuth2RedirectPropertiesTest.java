package dev.iamrat.auth.security.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2RedirectPropertiesTest {

    @Test
    @DisplayName("app.oauth2.redirect-url 설정을 바인딩한다")
    void bindsRedirectUrl() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app.oauth2.redirect-url", "https://front.example/oauth2/callback");

        Binder binder = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()));
        OAuth2RedirectProperties properties =
            binder.bind("app.oauth2", Bindable.of(OAuth2RedirectProperties.class)).get();

        assertThat(properties.getRedirectUrl())
            .isEqualTo("https://front.example/oauth2/callback");
    }
}
