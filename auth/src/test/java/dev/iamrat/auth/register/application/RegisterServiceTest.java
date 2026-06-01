package dev.iamrat.auth.register.application;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
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
    private AccountCommandService accountCommandService;

    @Mock
    private AccountQueryService accountQueryService;

    @InjectMocks
    private RegisterService registerService;

    private RegisterCommand createValidCommand() {
        return new RegisterCommand("testuser1", "Test1234!", "test@example.com", "길동이");
    }

    @Nested
    @DisplayName("회원가입 성공")
    class RegisterSuccess {

        @Test
        @DisplayName("모든 조건을 만족하면 계정을 생성하고 ID를 반환한다")
        void register_allConditionsMet_returnsAccountId() {
            RegisterCommand command = createValidCommand();

            given(accountQueryService.existsByUsername("testuser1")).willReturn(false);
            given(accountQueryService.existsByEmail("test@example.com")).willReturn(false);
            given(accountQueryService.existsByNickname("길동이")).willReturn(false);

            Account account = Account.builder()
                .username("testuser1")
                .email("test@example.com")
                .nickname("길동이")
                .build();

            given(accountCommandService.createGeneralAccount("testuser1", "Test1234!",
                "test@example.com", "길동이"))
                .willReturn(account);

            Long result = registerService.register(command);

            assertThat(result).isEqualTo(account.getId());
            verify(accountCommandService).createGeneralAccount("testuser1", "Test1234!",
                "test@example.com", "길동이");
        }

        @Test
        @DisplayName("회원가입 시 이메일을 정규화해서 계정 생성에 사용한다")
        void register_normalizesEmail() {
            RegisterCommand command = new RegisterCommand("testuser1", "Test1234!", " Test@Example.COM ", "길동이");

            given(accountQueryService.existsByUsername("testuser1")).willReturn(false);
            given(accountQueryService.existsByEmail("test@example.com")).willReturn(false);
            given(accountQueryService.existsByNickname("길동이")).willReturn(false);

            Account account = Account.builder()
                .username("testuser1")
                .email("test@example.com")
                .nickname("길동이")
                .build();

            given(accountCommandService.createGeneralAccount("testuser1", "Test1234!",
                "test@example.com", "길동이"))
                .willReturn(account);

            registerService.register(command);

            verify(accountCommandService).createGeneralAccount("testuser1", "Test1234!",
                "test@example.com", "길동이");
        }
    }

    @Nested
    @DisplayName("회원가입 실패")
    class RegisterFail {

        @Test
        @DisplayName("이미 존재하는 username이면 DUPLICATE_USERNAME 예외를 던진다")
        void register_duplicateUsername_throwsDuplicateUsername() {
            RegisterCommand command = createValidCommand();

            given(accountQueryService.existsByUsername("testuser1")).willReturn(true);

            assertThatThrownBy(() -> registerService.register(command))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.DUPLICATE_USERNAME));

            verify(accountCommandService, never()).createGeneralAccount(any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 DUPLICATE_EMAIL 예외를 던진다")
        void register_duplicateEmail_throwsDuplicateEmail() {
            RegisterCommand command = createValidCommand();

            given(accountQueryService.existsByUsername("testuser1")).willReturn(false);
            given(accountQueryService.existsByEmail("test@example.com")).willReturn(true);

            assertThatThrownBy(() -> registerService.register(command))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.DUPLICATE_EMAIL));

            verify(accountCommandService, never()).createGeneralAccount(any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 존재하는 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
        void register_duplicateNickname_throwsDuplicateNickname() {
            RegisterCommand command = createValidCommand();

            given(accountQueryService.existsByUsername("testuser1")).willReturn(false);
            given(accountQueryService.existsByEmail("test@example.com")).willReturn(false);
            given(accountQueryService.existsByNickname("길동이")).willReturn(true);

            assertThatThrownBy(() -> registerService.register(command))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.DUPLICATE_NICKNAME));

            verify(accountCommandService, never()).createGeneralAccount(any(), any(), any(), any());
        }
    }
}
