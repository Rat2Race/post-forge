package dev.iamrat.auth.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTest {

    @Mock
    private AccountStore accountStore;

    @InjectMocks
    private AccountQueryService accountQueryService;

    @Test
    @DisplayName("accountId로 roles가 포함된 계정을 조회한다")
    void findWithRolesById_existingAccount_returnsAccount() {
        Account account = Account.builder()
            .id(1L)
            .username("testuser1")
            .email("test@example.com")
            .nickname("tester")
            .provider("LOCAL")
            .build();

        given(accountStore.findWithRolesById(1L)).willReturn(Optional.of(account));

        assertThat(accountQueryService.findWithRolesById(1L)).isSameAs(account);
    }

    @Test
    @DisplayName("계정이 없으면 USER_NOT_FOUND 예외를 던진다")
    void findWithRolesById_missingAccount_throwsUserNotFound() {
        given(accountStore.findWithRolesById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountQueryService.findWithRolesById(1L))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("username으로 계정을 조회한다")
    void findByUsername_delegatesRepository() {
        Account account = Account.builder()
            .id(1L)
            .username("testuser1")
            .email("test@example.com")
            .nickname("tester")
            .provider("LOCAL")
            .build();

        given(accountStore.findByUsername("testuser1")).willReturn(Optional.of(account));

        assertThat(accountQueryService.findByUsername("testuser1")).containsSame(account);
    }

    @Test
    @DisplayName("OAuth provider identity로 계정을 조회한다")
    void findByProviderAndProviderId_delegatesRepository() {
        Account account = Account.builder()
            .id(1L)
            .username("oauth-user")
            .email("oauth@example.com")
            .nickname("oauthTester")
            .provider("GOOGLE")
            .providerId("google-user-123")
            .build();

        given(accountStore.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.of(account));

        assertThat(accountQueryService.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .containsSame(account);
    }

    @Test
    @DisplayName("email 존재 여부를 조회한다")
    void existsByEmail_delegatesRepository() {
        given(accountStore.existsByEmail("tester@test.com")).willReturn(true);

        assertThat(accountQueryService.existsByEmail("tester@test.com")).isTrue();
    }
}
