package dev.iamrat.auth.oauth.service;

import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.entity.Role;
import dev.iamrat.auth.account.repository.AccountRepository;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.auth.oauth.dto.OAuth2UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OAuth2AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private OAuth2UserInfo oAuth2UserInfo;

    @InjectMocks
    private OAuth2AccountService oAuth2AccountService;

    @Test
    @DisplayName("기존 계정이면 생성하지 않고 기존 계정을 반환한다")
    void getOrCreateAccount_existingAccount_returnsExistingAccount() {
        Account created = account("user-uuid-123", "tester");
        given(oAuth2UserInfo.getId()).willReturn("google-user-123");
        given(accountRepository.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.of(created));

        Account result = oAuth2AccountService.getOrCreateAccount("GOOGLE", oAuth2UserInfo);

        assertThat(result).isEqualTo(created);
        verify(accountService, never()).createAccount(
            anyString(), isNull(), anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    @DisplayName("신규 계정이면 닉네임을 생성해 계정을 만든다")
    void getOrCreateAccount_newAccount_createsAccount() {
        given(oAuth2UserInfo.getId()).willReturn("google-user-123");
        given(oAuth2UserInfo.getEmail()).willReturn("test@gmail.com");
        given(accountRepository.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.empty());
        given(accountRepository.existsByNickname(anyString())).willReturn(false);

        Account created = account("new-user-123", "newTester");
        given(accountService.createAccount(anyString(), isNull(), anyString(), anyString(), anyString(), anyString()))
            .willReturn(created);

        Account result = oAuth2AccountService.getOrCreateAccount("GOOGLE", oAuth2UserInfo);

        assertThat(result).isEqualTo(created);

        ArgumentCaptor<String> nicknameCaptor = ArgumentCaptor.forClass(String.class);
        verify(accountService).createAccount(
            eq("google_google-user-123"),
            isNull(),
            eq("test@gmail.com"),
            nicknameCaptor.capture(),
            eq("GOOGLE"),
            eq("google-user-123")
        );

        assertThat(nicknameCaptor.getValue())
            .startsWith("user_")
            .hasSizeLessThan(20);
    }

    private Account account(String userId, String nickname) {
        Account account = Account.builder()
            .userId(userId)
            .nickname(nickname)
            .email(userId + "@test.com")
            .provider("GOOGLE")
            .providerId("google-user-123")
            .build();
        account.addRole(Role.USER);
        return account;
    }
}
