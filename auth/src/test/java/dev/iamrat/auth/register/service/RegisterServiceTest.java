package dev.iamrat.auth.register.service;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.email.service.EmailVerificationService;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.auth.register.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private RegisterService registerService;

    private RegisterRequest createValidRequest() {
        return new RegisterRequest("testuser1", "Test1234!", "test@example.com", "길동이");
    }

    @Nested
    @DisplayName("회원가입 성공")
    class RegisterSuccess {

        @Test
        @DisplayName("모든 조건을 만족하면 계정을 생성하고 ID를 반환한다")
        void register_allConditionsMet_returnsAccountId() {
            RegisterRequest request = createValidRequest();

            given(emailVerificationService.isEmailVerified("test@example.com")).willReturn(true);
            given(accountService.existsByUserId("testuser1")).willReturn(false);

            Account account = Account.builder()
                .userId("testuser1")
                .email("test@example.com")
                .nickname("길동이")
                .build();

            given(accountService.createAccount("testuser1", "Test1234!",
                "test@example.com", "길동이", "LOCAL", null))
                .willReturn(account);

            Long result = registerService.register(request);

            assertThat(result).isEqualTo(account.getId());
            verify(accountService).createAccount("testuser1", "Test1234!",
                "test@example.com", "길동이", "LOCAL", null);
            verify(emailVerificationService).removeVerifiedEmail("test@example.com");
        }
    }

    @Nested
    @DisplayName("회원가입 실패")
    class RegisterFail {

        @Test
        @DisplayName("이메일 인증이 완료되지 않았으면 EMAIL_NOT_VERIFIED 예외를 던진다")
        void register_emailNotVerified_throwsEmailNotVerified() {
            RegisterRequest request = createValidRequest();

            given(emailVerificationService.isEmailVerified("test@example.com")).willReturn(false);

            assertThatThrownBy(() -> registerService.register(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.EMAIL_NOT_VERIFIED));

            verify(accountService, never()).createAccount(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 존재하는 아이디이면 DUPLICATE_ID 예외를 던진다")
        void register_duplicateUserId_throwsDuplicateId() {
            RegisterRequest request = createValidRequest();

            given(emailVerificationService.isEmailVerified("test@example.com")).willReturn(true);
            given(accountService.existsByUserId("testuser1")).willReturn(true);

            assertThatThrownBy(() -> registerService.register(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.DUPLICATE_ID));

            verify(accountService, never()).createAccount(any(), any(), any(), any(), any(), any());
        }
    }
}
