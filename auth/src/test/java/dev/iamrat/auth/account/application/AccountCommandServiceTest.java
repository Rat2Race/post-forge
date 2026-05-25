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
    @DisplayName("General 계정은 LOCAL provider와 인코딩된 비밀번호로 생성한다")
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
        assertThat(saved.getProvider()).isEqualTo("LOCAL");
        assertThat(saved.getProviderId()).isNull();
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getRoles()).containsExactly(AccountRole.USER);
    }

    @Test
    @DisplayName("OAuth 계정은 provider identity와 비밀번호 없음으로 생성한다")
    void createOAuthAccount_setsOAuthAccountFields() {
        given(accountStore.saveAndFlush(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

        Account result = accountCommandService.createOAuthAccount("GOOGLE", "google-user-123", " Test@Gmail.COM ", "oauthUser");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountStore).saveAndFlush(accountCaptor.capture());
        verify(passwordEncoder, never()).encode(any());

        Account saved = accountCaptor.getValue();
        assertThat(result).isSameAs(saved);
        assertThat(saved.getUsername()).isEqualTo("google_google-user-123");
        assertThat(saved.getPassword()).isNull();
        assertThat(saved.getEmail()).isEqualTo("test@gmail.com");
        assertThat(saved.getNickname()).isEqualTo("oauthUser");
        assertThat(saved.getProvider()).isEqualTo("GOOGLE");
        assertThat(saved.getProviderId()).isEqualTo("google-user-123");
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getRoles()).containsExactly(AccountRole.USER);
    }

    @Test
    @DisplayName("비활성 계정은 닉네임을 변경할 수 없다")
    void updateNickname_inactiveAccount_throwsAccountNotActive() {
        Account account = account(AccountStatus.SUSPENDED, "LOCAL", "encoded-password");

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
        Account account = account(AccountStatus.ACTIVE, "LOCAL", "encoded-password");

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
    @DisplayName("OAuth 계정은 provider 기준으로 비밀번호 변경을 차단한다")
    void updatePassword_oauthAccount_throwsOAuthPasswordChangeNotAllowed() {
        Account account = account(AccountStatus.ACTIVE, "GOOGLE", "encoded-password");

        given(accountStore.findWithRolesById(1L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> accountCommandService.updatePassword(1L, "old-password", "new-password"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_PASSWORD_UPDATE_NOT_ALLOWED));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Local 계정은 현재 비밀번호 검증 후 새 비밀번호 해시로 변경한다")
    void updatePassword_localAccount_updatesEncodedPassword() {
        Account account = account(AccountStatus.ACTIVE, "LOCAL", "old-encoded-password");

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
        Account account = account(AccountStatus.ACTIVE, "LOCAL", "encoded-password");

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

    private Account account(AccountStatus status, String provider, String password) {
        Account account = Account.builder()
            .id(1L)
            .username("testuser1")
            .password(password)
            .email("test@example.com")
            .nickname("tester")
            .provider(provider)
            .providerId("GOOGLE".equals(provider) ? "google-user-123" : null)
            .status(status)
            .build();
        account.addRole(AccountRole.USER);
        return account;
    }
}
