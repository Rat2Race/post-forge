package dev.iamrat.auth.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AccountCommandServiceTest {

    @Mock
    private AccountStore accountStore;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountCommandService accountCommandService;

    @BeforeEach
    void setUp() {
        accountCommandService = new AccountCommandService(accountStore, passwordEncoder);
    }

    @Test
    @DisplayName("General 계정은 인코딩된 비밀번호와 기본 상태로 생성한다")
    void createGeneralAccount_setsLocalAccountFields() {
        given(passwordEncoder.encode("Test1234!")).willReturn("encoded-password");
        given(accountStore.saveAndFlush(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

        Account result = accountCommandService.createGeneralAccount("testuser1", "Test1234!", " Test@Example.COM ", "길동이");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountStore).saveAndFlush(accountCaptor.capture());
        Account saved = accountCaptor.getValue();

        assertThat(result).isSameAs(saved);
        assertThat(saved.getUsername()).isEqualTo("testuser1");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getNickname()).isEqualTo("길동이");
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getRoles()).containsExactly(AccountRole.USER);
    }

    @Test
    @DisplayName("비활성 계정은 닉네임을 변경할 수 없다")
    void updateNickname_inactiveAccount_throwsAccountNotActive() {
        Account account = account(AccountStatus.SUSPENDED, "encoded-password");

        given(accountStore.findWithRolesById(1L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> accountCommandService.updateNickname(1L, "newNick"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE));

        assertThat(account.getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
    void updateNickname_duplicateNickname_throwsDuplicateNickname() {
        Account account = account(AccountStatus.ACTIVE, "encoded-password");

        given(accountStore.findWithRolesById(1L)).willReturn(Optional.of(account));
        given(accountStore.existsByNickname("newNick")).willReturn(true);

        assertThatThrownBy(() -> accountCommandService.updateNickname(1L, "newNick"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.DUPLICATE_NICKNAME));

        assertThat(account.getNickname()).isEqualTo("tester");
        verify(accountStore, never()).flush();
    }

    @Test
    @DisplayName("Local 계정은 현재 비밀번호 검증 후 새 비밀번호 해시로 변경한다")
    void updatePassword_localAccount_updatesEncodedPassword() {
        Account account = account(AccountStatus.ACTIVE, "old-encoded-password");

        given(accountStore.findWithRolesById(1L)).willReturn(Optional.of(account));
        given(passwordEncoder.matches("old-password", "old-encoded-password")).willReturn(true);
        given(passwordEncoder.encode("new-password")).willReturn("new-encoded-password");

        accountCommandService.updatePassword(1L, "old-password", "new-password");

        assertThat(account.getPassword()).isEqualTo("new-encoded-password");
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    @DisplayName("계정 상태 변경 성공 - 상태를 변경한다")
    void updateStatus_changesAccountStatus() {
        Account account = account(AccountStatus.ACTIVE, "encoded-password");

        given(accountStore.findWithRolesById(1L)).willReturn(Optional.of(account));

        accountCommandService.updateStatus(1L, AccountStatus.SUSPENDED);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    @DisplayName("계정 상태 변경 실패 - status가 null이면 INVALID_ACCOUNT_STATUS 예외를 던진다")
    void updateStatus_nullStatus_throwsInvalidAccountStatus() {
        assertThatThrownBy(() -> accountCommandService.updateStatus(1L, null))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_ACCOUNT_STATUS));

        verify(accountStore, never()).findWithRolesById(any());
    }

    @Test
    @DisplayName("계정 상태 변경 실패 - 계정이 없으면 USER_NOT_FOUND 예외를 던진다")
    void updateStatus_accountNotFound_throwsUserNotFound() {
        given(accountStore.findWithRolesById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountCommandService.updateStatus(1L, AccountStatus.SUSPENDED))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND));
    }

    private Account account(AccountStatus status, String password) {
        Account account = Account.builder()
            .id(1L)
            .username("testuser1")
            .password(password)
            .email("test@example.com")
            .nickname("tester")
            .status(status)
            .build();
        account.addRole(AccountRole.USER);
        return account;
    }
}
