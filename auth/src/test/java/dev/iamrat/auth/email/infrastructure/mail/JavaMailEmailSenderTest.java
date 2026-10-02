package dev.iamrat.auth.email.infrastructure.mail;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
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

    @Test
    @DisplayName("메일 발송 로그에는 수신자 주소와 일회용 인증 토큰을 남기지 않는다")
    void sendVerificationEmail_doesNotLogRecipientOrToken(CapturedOutput output) {
        EmailVerificationProperties emailVerificationProperties = new EmailVerificationProperties();
        emailVerificationProperties.setVerificationBaseUrl("https://front.example/email/verify");
        MailSenderProperties mailSenderProperties = new MailSenderProperties();
        mailSenderProperties.setUsername("noreply@example.com");
        JavaMailEmailSender emailSender = new JavaMailEmailSender(mailSender, emailVerificationProperties, mailSenderProperties);
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage(Session.getInstance(new Properties())));
        Logger logger = (Logger) LoggerFactory.getLogger(JavaMailEmailSender.class);
        Level previous = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        try {
            emailSender.sendVerificationEmail("leak@example.com", "leak-token-123");
        } finally {
            logger.setLevel(previous);
        }

        assertThat(output.getAll()).doesNotContain("leak@example.com").doesNotContain("leak-token-123");
    }

    @Test
    @DisplayName("메일 작성 중 MessagingException이 나도 이메일 발송 실패로 바꾸고, 예외 메시지(주소·토큰)는 로그에 남기지 않는다")
    void messagingFailure_becomesEmailSendFailedWithoutLeakingMessage(CapturedOutput output) {
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage(Session.getInstance(new Properties())) {
            @Override
            public void setContent(Multipart multipart) throws MessagingException {
                throw new MessagingException("leak@example.invalid secret-token-123");
            }
        });

        assertThatThrownBy(() -> sender().sendVerificationEmail("leak@example.invalid", "secret-token-123"))
            .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_SEND_FAILED));
        assertThat(output.getAll()).doesNotContain("leak@example.invalid").doesNotContain("secret-token-123");
    }

    @Test
    @DisplayName("메일 서버 런타임 오류(MailException)도 이메일 발송 실패로 바꾸고, 예외 메시지는 로그에 남기지 않는다")
    void mailServerFailure_becomesEmailSendFailedWithoutLeakingMessage(CapturedOutput output) {
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage(Session.getInstance(new Properties())));
        willThrow(new MailSendException("leak@example.invalid secret-token-123")).given(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender().sendVerificationEmail("leak@example.invalid", "secret-token-123"))
            .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_SEND_FAILED));
        assertThat(output.getAll()).doesNotContain("leak@example.invalid").doesNotContain("secret-token-123");
    }

    private JavaMailEmailSender sender() {
        EmailVerificationProperties emailVerificationProperties = new EmailVerificationProperties();
        emailVerificationProperties.setVerificationBaseUrl("https://front.example/email/verify");
        MailSenderProperties mailSenderProperties = new MailSenderProperties();
        mailSenderProperties.setUsername("noreply@example.com");
        return new JavaMailEmailSender(mailSender, emailVerificationProperties, mailSenderProperties);
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
