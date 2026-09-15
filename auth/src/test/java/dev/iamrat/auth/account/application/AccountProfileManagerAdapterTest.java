package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.account.AccountProfileDetails;
import dev.iamrat.core.global.exception.CustomException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
class AccountProfileManagerAdapterTest {

    @Mock
    private AccountQueryService accountQueryService;

    @Mock
    private AccountCommandService accountCommandService;

    @InjectMocks
    private AccountProfileManagerAdapter accountProfileManager;

    @Test
    @DisplayName("LOCAL 계정이면 isOAuthUser가 false이고 roles가 정렬되어 반환된다")
    void getProfile_localAccount_returnsDetailsWithSortedRoles() {
        Account account = account(AccountStatus.ACTIVE, "LOCAL",
            Set.of(AccountRole.USER, AccountRole.ADMIN, AccountRole.MANAGER));
        given(accountQueryService.findWithRolesById(1L)).willReturn(Optional.of(account));

        AccountProfileDetails details = accountProfileManager.getProfile(1L);

        assertThat(details.accountId()).isEqualTo(1L);
        assertThat(details.nickname()).isEqualTo("tester");
        assertThat(details.isOAuthUser()).isFalse();
        assertThat(details.roles()).containsExactly("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_USER");
    }

    @Test
    @DisplayName("LOCAL이 아닌 provider면 isOAuthUser가 true다")
    void getProfile_oauthAccount_isOAuthUserTrue() {
        Account account = account(AccountStatus.ACTIVE, "KAKAO", Set.of(AccountRole.USER));
        given(accountQueryService.findWithRolesById(1L)).willReturn(Optional.of(account));

        AccountProfileDetails details = accountProfileManager.getProfile(1L);

        assertThat(details.isOAuthUser()).isTrue();
    }

    @Test
    @DisplayName("비활성 계정이면 프로필을 반환하지 않는다")
    void getProfile_inactiveAccount_throwsAccountNotActive() {
        Account account = account(AccountStatus.SUSPENDED, "LOCAL", Set.of(AccountRole.USER));
        given(accountQueryService.findWithRolesById(1L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> accountProfileManager.getProfile(1L))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("계정이 없으면 USER_NOT_FOUND 예외가 발생한다")
    void getProfile_accountNotFound_throwsUserNotFound() {
        given(accountQueryService.findWithRolesById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountProfileManager.getProfile(1L))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }

    private Account account(AccountStatus status, String provider, Set<AccountRole> roles) {
        return Account.builder()
            .id(1L)
            .username("testuser1")
            .email("test@example.com")
            .nickname("tester")
            .provider(provider)
            .status(status)
            .roles(new HashSet<>(roles))
            .build();
    }
}
