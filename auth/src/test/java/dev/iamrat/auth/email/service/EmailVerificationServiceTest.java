package dev.iamrat.auth.email.service;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @Mock
    AccountRepository accountRepository;

    @Mock
    EmailService emailService;

    @InjectMocks
    EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("이미 가입된 이메일로 인증 요청하면 DUPLICATE_EMAIL 예외를 발생한다")
    void sendEmail_duplicate_throwsDuplicateEmail() {
        String mockEmail = "tester@test.com";

        given(accountRepository.existsByEmail(mockEmail)).willReturn(true);

        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(mockEmail))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("올바른 토큰이 아니면 EMAIL_CODE_NOT_FOUND 예외를 발생한다")
    void verify_invalidToken_throwsCodeNotFound() {
        String token = "쓰레기-토큰-입니다";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete("email_verify_token:" + token)).willReturn(null);

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(token))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.EMAIL_CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("유효한 토큰이면 이메일을 반환하고 인증 완료 상태를 저장한다")
    void verify_validToken_returnsEmail() {
        String token = "valid-token";
        String email = "tester@test.com";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete("email_verify_token:" + token)).willReturn(email);

        String result = emailVerificationService.verifyEmail(token);

        assertThat(result).isEqualTo(email);
        verify(valueOperations).set("email_verified:" + email, "true", 1L, java.util.concurrent.TimeUnit.HOURS);
    }
}
