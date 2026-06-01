package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AccountProfileReaderAdapterTest {

    @Mock
    private AccountStore accountStore;

    @InjectMocks
    private AccountProfileReaderAdapter accountProfileReader;

    @Test
    @DisplayName("accountId로 게시판이 필요한 계정 프로필을 반환한다")
    void getProfile_activeAccount_returnsProfile() {
        given(accountStore.findById(1L)).willReturn(Optional.of(account(AccountStatus.ACTIVE)));

        AccountProfile profile = accountProfileReader.getProfile(1L);

        assertThat(profile.accountId()).isEqualTo(1L);
        assertThat(profile.nickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("비활성 계정이면 프로필을 반환하지 않는다")
    void getProfile_inactiveAccount_throwsAccountNotActive() {
        given(accountStore.findById(1L)).willReturn(Optional.of(account(AccountStatus.SUSPENDED)));

        assertThatThrownBy(() -> accountProfileReader.getProfile(1L))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    private Account account(AccountStatus status) {
        return Account.builder()
            .id(1L)
            .username("testuser1")
            .email("test@example.com")
            .nickname("tester")
            .status(status)
            .build();
    }
}
