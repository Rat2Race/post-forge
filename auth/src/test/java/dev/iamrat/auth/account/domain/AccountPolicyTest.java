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
        assertThatCode(() -> accountPolicy.requireActive(account(AccountStatus.ACTIVE)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비활성 계정은 ACCOUNT_NOT_ACTIVE 예외를 던진다")
    void requireActive_inactiveAccount_throwsAccountNotActive() {
        assertThatThrownBy(() -> accountPolicy.requireActive(account(AccountStatus.SUSPENDED)))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE));
    }

    @Test
    @DisplayName("null 상태는 INVALID_ACCOUNT_STATUS 예외를 던진다")
    void requireStatus_nullStatus_throwsInvalidAccountStatus() {
        assertThatThrownBy(() -> accountPolicy.requireStatus(null))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_ACCOUNT_STATUS));
    }

    private Account account(AccountStatus status) {
        return Account.builder()
            .username("testuser1")
            .password("encoded-password")
            .email("test@example.com")
            .nickname("tester")
            .status(status)
            .build();
    }
}
