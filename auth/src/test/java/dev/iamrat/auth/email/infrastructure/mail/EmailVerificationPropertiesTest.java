package dev.iamrat.auth.email.infrastructure.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationPropertiesTest {

    @Test
    @DisplayName("app.email.verification-base-url 설정을 바인딩한다")
    void bindsVerificationBaseUrl() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app.email.verification-base-url", "https://front.example/email/verify");

        Binder binder = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()));
        EmailVerificationProperties properties =
            binder.bind("app.email", Bindable.of(EmailVerificationProperties.class)).get();

        assertThat(properties.getVerificationBaseUrl())
            .isEqualTo("https://front.example/email/verify");
    }
}
