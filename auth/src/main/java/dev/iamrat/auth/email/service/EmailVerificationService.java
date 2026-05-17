package dev.iamrat.auth.email.service;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final String TOKEN_TO_EMAIL_PREFIX = "email_verify_token:";
    private static final String EMAIL_VERIFIED_PREFIX = "email_verified:";
    private static final long TOKEN_TTL_MINUTES = 30;
    private static final long VERIFIED_TTL_HOURS = 1;

    private final RedisTemplate<String, String> redisTemplate;
    private final AccountRepository accountRepository;
    private final EmailService emailService;

    public void sendVerificationEmail(String email) {
        if (accountRepository.existsByEmail(email)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        String token = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                TOKEN_TO_EMAIL_PREFIX + token,
                email,
                TOKEN_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        emailService.sendVerificationEmail(email, token);
    }

    public String verifyEmail(String token) {
        String key = TOKEN_TO_EMAIL_PREFIX + token;
        String email = redisTemplate.opsForValue().getAndDelete(key);

        if (email == null) {
            throw new CustomException(AuthErrorCode.EMAIL_CODE_NOT_FOUND);
        }

        redisTemplate.opsForValue().set(
                EMAIL_VERIFIED_PREFIX + email,
                "true",
                VERIFIED_TTL_HOURS,
                TimeUnit.HOURS
        );

        return email;
    }

    public boolean isEmailVerified(String email) {
        return Boolean.TRUE.toString().equals(
                redisTemplate.opsForValue().get(EMAIL_VERIFIED_PREFIX + email)
        );
    }

    public void removeVerifiedEmail(String email) {
        redisTemplate.delete(EMAIL_VERIFIED_PREFIX + email);
    }
}
