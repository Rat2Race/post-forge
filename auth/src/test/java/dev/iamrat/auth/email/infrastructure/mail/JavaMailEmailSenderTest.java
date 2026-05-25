package dev.iamrat.auth.email.infrastructure.mail;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JavaMailEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    @DisplayName("인증 메일은 설정된 발신자와 인증 URL을 사용해 발송한다")
    void sendVerificationEmail_usesSenderAndVerificationUrl() throws Exception {
        EmailVerificationProperties emailVerificationProperties = new EmailVerificationProperties();
        emailVerificationProperties.setVerificationBaseUrl("https://front.example/email/verify");
        MailSenderProperties mailSenderProperties = new MailSenderProperties();
        mailSenderProperties.setUsername("noreply@example.com");
        JavaMailEmailSender emailSender = new JavaMailEmailSender(
            mailSender,
            emailVerificationProperties,
            mailSenderProperties
        );
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        given(mailSender.createMimeMessage()).willReturn(message);

        emailSender.sendVerificationEmail("tester@example.com", "email-token");

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("PostForge 이메일 인증");
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("noreply@example.com");
        assertThat(from.getPersonal()).isEqualTo("PostForge");
        InternetAddress recipient = (InternetAddress) message.getAllRecipients()[0];
        assertThat(recipient.getAddress()).isEqualTo("tester@example.com");
        assertThat(messageContent(message))
            .contains("https://front.example/email/verify?token=email-token");
    }

    private String messageContent(MimeMessage message) throws Exception {
        return contentText(message.getContent());
    }

    private String contentText(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                result.append(contentText(bodyPart.getContent()));
            }
            return result.toString();
        }
        return "";
    }
}
