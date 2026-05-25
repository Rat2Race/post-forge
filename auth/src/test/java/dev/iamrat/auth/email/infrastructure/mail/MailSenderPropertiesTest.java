package dev.iamrat.auth.email.infrastructure.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class MailSenderPropertiesTest {

    @Test
    @DisplayName("spring.mail prefix로 메일 발신자 설정을 바인딩한다")
    void mailSenderProperties_usesSpringMailPrefix() {
        ConfigurationProperties annotation =
            MailSenderProperties.class.getAnnotation(ConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("spring.mail");
    }

    @Test
    @DisplayName("메일 발신자 기본값을 유지한다")
    void mailSenderProperties_defaultUsername() {
        MailSenderProperties properties = new MailSenderProperties();

        assertThat(properties.getUsername()).isEqualTo("noreply@postforge.dev");
    }
}
