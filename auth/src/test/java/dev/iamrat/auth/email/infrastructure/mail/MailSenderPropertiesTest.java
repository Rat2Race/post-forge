package dev.iamrat.auth.email.infrastructure.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailSenderPropertiesTest {

    @Test
    @DisplayName("메일 발신자 기본값을 유지한다")
    void mailSenderProperties_defaultUsername() {
        MailSenderProperties properties = new MailSenderProperties();

        assertThat(properties.getUsername()).isEqualTo("noreply@postforge.dev");
    }
}
