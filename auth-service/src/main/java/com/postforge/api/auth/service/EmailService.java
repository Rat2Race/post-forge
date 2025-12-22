package com.postforge.api.auth.service;

import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
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
    private final JavaMailSender mailsender;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
//    //local
    @Value("${spring.cors.allowed-origins}")
    private String allowedOrigins;
    
//    //cloud
//    @Value("${cors.allowed-origins}")
//    private String allowedOrigins;

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = allowedOrigins + "/verify-email?token=" + token;

        if ("dev".equals(activeProfile) || "local".equals(activeProfile)) {
            log.info("========================================");
            log.info("이메일 인증 링크 (개발 모드)");
            log.info("수신자: {}", toEmail);
            log.info("인증 URL: {}", verificationUrl);
            log.info("========================================");
            return;
        }

        try {
            MimeMessage message = mailsender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("PostForge 이메일 인증");
            helper.setFrom("PostForge <rlaalstlr2001@gmail.com>");

            String htmlContent = loadHtmlTemplate(verificationUrl);

            helper.setText(htmlContent, true);

            mailsender.send(message);

        } catch (MessagingException e) {
            log.error("이메일 발송 실패", e);
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String loadHtmlTemplate(String verificationUrl) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email-verification.html");
            String template = StreamUtils.copyToString(
                resource.getInputStream(),
                StandardCharsets.UTF_8
            );

//            String imageUrl = "http://localhost:8080/images/tetonam.png";

            return template
                .replace("{{VERIFICATION_URL}}", verificationUrl);
//                .replace("{{IMAGE_URL}}", imageUrl);

        } catch (IOException e) {
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * 만료시간 지난 토큰 삭제 로직이 필요할거 같긴 함
     */
}
