package dev.iamrat.auth.oauth.application;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.account.application.AccountQueryService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OAuth2AccountServiceTest {

    @Mock
    private AccountQueryService accountQueryService;

    @Mock
    private AccountCommandService accountService;

    @Mock
    private OAuth2UserProfile oAuth2UserProfile;

    @InjectMocks
    private OAuth2AccountService oAuth2AccountService;

    @Test
    @DisplayName("기존 계정이면 생성하지 않고 기존 계정을 반환한다")
    void getOrCreateAccount_existingAccount_returnsExistingAccount() {
        Account created = account("user-uuid-123", "tester");
        given(oAuth2UserProfile.getId()).willReturn("google-user-123");
        given(accountQueryService.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.of(created));

        Account result = oAuth2AccountService.getOrCreateAccount("GOOGLE", oAuth2UserProfile);

        assertThat(result).isEqualTo(created);
        verify(accountService, never()).createOAuthAccount(
            anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    @DisplayName("신규 계정이면 닉네임을 생성해 계정을 만든다")
    void getOrCreateAccount_newAccount_createsAccount() {
        given(oAuth2UserProfile.getId()).willReturn("google-user-123");
        given(oAuth2UserProfile.getEmail()).willReturn("test@gmail.com");
        given(accountQueryService.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.<Account>empty());
        given(accountQueryService.existsByNickname(anyString())).willReturn(false);

        Account created = account("new-user-123", "newTester");
        given(accountService.createOAuthAccount(anyString(), anyString(), anyString(), anyString()))
            .willReturn(created);

        Account result = oAuth2AccountService.getOrCreateAccount("GOOGLE", oAuth2UserProfile);

        assertThat(result).isEqualTo(created);

        ArgumentCaptor<String> nicknameCaptor = ArgumentCaptor.forClass(String.class);
        verify(accountService).createOAuthAccount(
            eq("GOOGLE"),
            eq("google-user-123"),
            eq("test@gmail.com"),
            nicknameCaptor.capture()
        );

        assertThat(nicknameCaptor.getValue())
            .startsWith("user_")
            .hasSizeLessThan(20);
    }

    @Test
    @DisplayName("동시 OAuth 계정 생성 충돌이면 이미 생성된 계정을 다시 조회해 반환한다")
    void getOrCreateAccount_concurrentCreateConflict_returnsExistingAccount() {
        given(oAuth2UserProfile.getId()).willReturn("google-user-123");
        given(oAuth2UserProfile.getEmail()).willReturn("test@gmail.com");
        Account createdByConcurrentRequest = account("concurrent-user-123", "concurrentTester");
        given(accountQueryService.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.<Account>empty())
            .willReturn(Optional.of(createdByConcurrentRequest));
        given(accountQueryService.existsByNickname(anyString())).willReturn(false);
        given(accountService.createOAuthAccount(anyString(), anyString(), anyString(), anyString()))
            .willThrow(new DataIntegrityViolationException("duplicate provider identity"));

        Account result = oAuth2AccountService.getOrCreateAccount("GOOGLE", oAuth2UserProfile);

        assertThat(result).isEqualTo(createdByConcurrentRequest);
        verify(accountQueryService, times(2)).findByProviderAndProviderId("GOOGLE", "google-user-123");
    }

    @Test
    @DisplayName("OAuth 생성 실패 후 같은 provider identity 계정이 없으면 원래 무결성 예외를 유지한다")
    void getOrCreateAccount_integrityViolationWithoutConcurrentAccount_rethrows() {
        given(oAuth2UserProfile.getId()).willReturn("google-user-123");
        given(oAuth2UserProfile.getEmail()).willReturn("test@gmail.com");
        given(accountQueryService.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.<Account>empty())
            .willReturn(Optional.<Account>empty());
        given(accountQueryService.existsByNickname(anyString())).willReturn(false);
        DataIntegrityViolationException exception =
            new DataIntegrityViolationException("duplicate nickname");
        given(accountService.createOAuthAccount(anyString(), anyString(), anyString(), anyString()))
            .willThrow(exception);

        assertThatThrownBy(() -> oAuth2AccountService.getOrCreateAccount("GOOGLE", oAuth2UserProfile))
            .isSameAs(exception);
    }

    private Account account(String username, String nickname) {
        Account account = Account.builder()
            .username(username)
            .nickname(nickname)
            .email(username + "@test.com")
            .provider("GOOGLE")
            .providerId("google-user-123")
            .build();
        account.addRole(AccountRole.USER);
        return account;
    }
}
