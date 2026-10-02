package dev.iamrat.auth.email.infrastructure.mail;

import dev.iamrat.auth.email.application.EmailSender;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailSender implements EmailSender {
    private final JavaMailSender mailSender;
    private final EmailVerificationProperties emailVerificationProperties;
    private final MailSenderProperties mailSenderProperties;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = emailVerificationProperties.getVerificationBaseUrl() + "?token=" + token;

        // 수신자 주소는 개인정보이고 인증 URL에는 일회용 토큰이 있어 둘 다 로그에 남기지 않는다.
        log.info("이메일 인증 링크 발송");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("PostForge 이메일 인증");
            helper.setFrom("PostForge <" + mailSenderProperties.getUsername() + ">");

            String htmlContent = loadHtmlTemplate(verificationUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MailException | MessagingException e) {
            // 메일 예외 메시지에는 수신자 주소·인증 링크가 실릴 수 있어 실패 종류만 남긴다.
            log.error("이메일 발송 실패: {}", e.getClass().getSimpleName());
            throw new CustomException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String loadHtmlTemplate(String verificationUrl) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email-verification.html");
            String template = StreamUtils.copyToString(
                resource.getInputStream(),
                StandardCharsets.UTF_8
            );
            return template.replace("{{VERIFICATION_URL}}", verificationUrl);

        } catch (IOException e) {
            log.error("이메일 템플릿 로딩 실패", e);
            throw new CustomException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
