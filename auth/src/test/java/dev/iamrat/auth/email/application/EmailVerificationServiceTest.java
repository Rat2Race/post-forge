package dev.iamrat.auth.email.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    AccountQueryService accountQueryService;

    @Mock
    EmailVerificationStore emailVerificationStore;

    @Mock
    EmailSender emailSender;

    @InjectMocks
    EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("이미 가입된 이메일로 인증 요청하면 DUPLICATE_EMAIL 예외를 발생한다")
    void sendEmail_duplicate_throwsDuplicateEmail() {
        String mockEmail = "tester@test.com";

        given(accountQueryService.existsByEmail(mockEmail)).willReturn(true);

        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(mockEmail))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("인증 메일 발송 시 이메일을 정규화해서 중복 확인, 토큰 저장, 발송에 사용한다")
    void sendEmail_normalizesEmail() {
        String rawEmail = " Tester@Test.COM ";
        String normalizedEmail = "tester@test.com";

        given(accountQueryService.existsByEmail(normalizedEmail)).willReturn(false);

        emailVerificationService.sendVerificationEmail(rawEmail);

        verify(accountQueryService).existsByEmail(normalizedEmail);
        verify(emailVerificationStore).saveToken(anyString(), eq(normalizedEmail));
        verify(emailSender).sendVerificationEmail(eq(normalizedEmail), anyString());
    }

    @Test
    @DisplayName("올바른 토큰이 아니면 EMAIL_CODE_NOT_FOUND 예외를 발생한다")
    void verify_invalidToken_throwsCodeNotFound() {
        String token = "쓰레기-토큰-입니다";

        given(emailVerificationStore.getEmailAndDeleteToken(token)).willReturn(null);

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
        String email = " Tester@Test.COM ";
        String normalizedEmail = "tester@test.com";

        given(emailVerificationStore.getEmailAndDeleteToken(token)).willReturn(email);

        String result = emailVerificationService.verifyEmail(token);

        assertThat(result).isEqualTo(normalizedEmail);
        verify(emailVerificationStore).markVerified(normalizedEmail);
    }

    @Test
    @DisplayName("인증 완료 여부는 정규화된 이메일로 조회한다")
    void isEmailVerified_normalizesEmail() {
        given(emailVerificationStore.isVerified("tester@test.com")).willReturn(true);

        assertThat(emailVerificationService.isEmailVerified(" Tester@Test.COM ")).isTrue();
    }
}
