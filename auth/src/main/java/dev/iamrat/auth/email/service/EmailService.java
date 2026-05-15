package dev.iamrat.auth.email.service;

import dev.iamrat.auth.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.security.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Value("${spring.mail.username:noreply@postforge.dev}")
    private String senderEmail;

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = appProperties.getEmail().getVerificationBaseUrl() + "?token=" + token;

        log.info("이메일 인증 링크 발송 - 수신자: {}", toEmail);
        log.debug("인증 URL: {}", verificationUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("PostForge 이메일 인증");
            helper.setFrom("PostForge <" + senderEmail + ">");

            String htmlContent = loadHtmlTemplate(verificationUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            log.error("이메일 발송 실패 - 수신자: {}", toEmail, e);
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
