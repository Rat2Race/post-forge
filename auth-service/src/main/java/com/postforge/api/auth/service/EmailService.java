package com.postforge.api.auth.service;

import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailsender;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailsender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("PostForge 이메일 인증");
            helper.setFrom("PostForge <rlaalstlr2001@gmail.com>");

            String verificationUrl = "http://localhost:3000/verify-email?token=" + token;
            String htmlContent = loadHtmlTemplate(verificationUrl);

            helper.setText(htmlContent, true);

            mailsender.send(message);

        } catch (MessagingException e) {
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
}
