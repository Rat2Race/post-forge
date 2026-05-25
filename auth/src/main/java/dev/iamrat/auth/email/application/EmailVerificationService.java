package dev.iamrat.auth.email.application;

import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.support.normalizer.EmailNormalizer;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final AccountQueryService accountQueryService;
    private final EmailVerificationStore emailVerificationStore;
    private final EmailSender emailSender;

    public void sendVerificationEmail(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        if (accountQueryService.existsByEmail(normalizedEmail)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        String token = UUID.randomUUID().toString();

        emailVerificationStore.saveToken(token, normalizedEmail);
        emailSender.sendVerificationEmail(normalizedEmail, token);
    }

    public String verifyEmail(String token) {
        String email = emailVerificationStore.getEmailAndDeleteToken(token);

        if (email == null) {
            throw new CustomException(AuthErrorCode.EMAIL_CODE_NOT_FOUND);
        }

        String normalizedEmail = EmailNormalizer.normalize(email);

        emailVerificationStore.markVerified(normalizedEmail);

        return normalizedEmail;
    }

    public boolean isEmailVerified(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        return emailVerificationStore.isVerified(normalizedEmail);
    }

    public void removeVerifiedEmail(String email) {
        emailVerificationStore.removeVerified(EmailNormalizer.normalize(email));
    }
}
