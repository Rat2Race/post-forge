package dev.iamrat.auth.account.domain;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountPolicyTest {

    private final AccountPolicy accountPolicy = new AccountPolicy();

    @Test
    @DisplayName("활성 계정은 통과한다")
    void requireActive_activeAccount_passes() {
        assertThatCode(() -> accountPolicy.requireActive(account(AccountStatus.ACTIVE, Account.LOCAL_PROVIDER)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비활성 계정은 ACCOUNT_NOT_ACTIVE 예외를 던진다")
    void requireActive_inactiveAccount_throwsAccountNotActive() {
        assertThatThrownBy(() -> accountPolicy.requireActive(account(AccountStatus.SUSPENDED, Account.LOCAL_PROVIDER)))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE));
    }

    @Test
    @DisplayName("로컬 계정은 비밀번호 변경 정책을 통과한다")
    void requireLocalAccount_localAccount_passes() {
        assertThatCode(() -> accountPolicy.requireLocalAccount(account(AccountStatus.ACTIVE, Account.LOCAL_PROVIDER)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("OAuth 계정은 비밀번호 변경 정책에서 거절된다")
    void requireLocalAccount_oauthAccount_throwsOAuthPasswordUpdateNotAllowed() {
        assertThatThrownBy(() -> accountPolicy.requireLocalAccount(account(AccountStatus.ACTIVE, "GOOGLE")))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_PASSWORD_UPDATE_NOT_ALLOWED));
    }

    @Test
    @DisplayName("null 상태는 INVALID_ACCOUNT_STATUS 예외를 던진다")
    void requireStatus_nullStatus_throwsInvalidAccountStatus() {
        assertThatThrownBy(() -> accountPolicy.requireStatus(null))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_ACCOUNT_STATUS));
    }

    private Account account(AccountStatus status, String provider) {
        return Account.builder()
            .username("testuser1")
            .password("encoded-password")
            .email("test@example.com")
            .nickname("tester")
            .provider(provider)
            .providerId(Account.LOCAL_PROVIDER.equals(provider) ? null : "provider-id")
            .status(status)
            .build();
    }
}
