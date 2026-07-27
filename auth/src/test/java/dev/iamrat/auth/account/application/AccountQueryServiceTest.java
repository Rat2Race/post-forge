package dev.iamrat.auth.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import dev.iamrat.auth.account.domain.Account;
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
    @DisplayName("계정 ID로 권한이 포함된 계정을 조회한다")
    void findWithRolesById_existingAccount_returnsOptionalAccount() {
        Account account = Account.builder()
            .id(1L)
            .username("testuser1")
            .email("test@example.com")
            .nickname("tester")
            .provider("LOCAL")
            .build();

        given(accountStore.findWithRolesById(1L)).willReturn(Optional.of(account));

        assertThat(accountQueryService.findWithRolesById(1L)).containsSame(account);
    }

    @Test
    @DisplayName("권한 포함 계정 조회 결과가 없으면 빈 Optional을 반환한다")
    void findWithRolesById_missingAccount_returnsEmptyOptional() {
        given(accountStore.findWithRolesById(1L)).willReturn(Optional.empty());

        assertThat(accountQueryService.findWithRolesById(1L)).isEmpty();
    }

    @Test
    @DisplayName("계정 ID로 계정을 조회한다")
    void findById_existingAccount_returnsOptionalAccount() {
        Account account = Account.builder()
            .id(1L)
            .username("testuser1")
            .email("test@example.com")
            .nickname("tester")
            .provider("LOCAL")
            .build();

        given(accountStore.findById(1L)).willReturn(Optional.of(account));

        assertThat(accountQueryService.findById(1L)).containsSame(account);
    }

    @Test
    @DisplayName("계정 조회 결과가 없으면 빈 Optional을 반환한다")
    void findById_missingAccount_returnsEmptyOptional() {
        given(accountStore.findById(1L)).willReturn(Optional.empty());

        assertThat(accountQueryService.findById(1L)).isEmpty();
    }

    @Test
    @DisplayName("사용자명으로 계정을 조회한다")
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
    @DisplayName("OAuth 제공자 식별자로 계정을 조회한다")
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
    @DisplayName("이메일 존재 여부를 조회한다")
    void existsByEmail_delegatesRepository() {
        given(accountStore.existsByEmail("tester@test.com")).willReturn(true);

        assertThat(accountQueryService.existsByEmail("tester@test.com")).isTrue();
    }
}
