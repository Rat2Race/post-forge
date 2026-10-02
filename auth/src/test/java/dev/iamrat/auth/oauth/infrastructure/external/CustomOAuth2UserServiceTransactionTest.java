package dev.iamrat.auth.oauth.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.application.AccountStore;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.infrastructure.persistence.AccountPersistenceAdapter;
import dev.iamrat.auth.account.infrastructure.persistence.AccountRepository;
import dev.iamrat.auth.oauth.application.OAuth2AccountService;
import dev.iamrat.auth.security.infrastructure.principal.CustomOAuth2User;
import dev.iamrat.auth.token.application.RefreshTokenStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 같은 사용자의 OAuth 로그인 두 개가 동시에 가입을 시도한 경우를 실제 PostgreSQL에서 재현한다.
 * PostgreSQL은 문장 하나가 실패하면 그 트랜잭션의 다음 문장을 모두 거절한다(25P02). 그래서 충돌 뒤 재조회는 새 트랜잭션에서 돌아야 한다.
 */
@Tag("integration")
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    AccountPersistenceAdapter.class,
    AccountCommandService.class,
    OAuth2AccountService.class,
    CustomOAuth2UserServiceTransactionTest.Config.class
})
class CustomOAuth2UserServiceTransactionTest {

    private static final String PROVIDER_ID = "google-tx-boundary";
    private static final String EMAIL = "tx-boundary@gmail.com";

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    void cleanUp() {
        accountRepository.findByProviderAndProviderId("GOOGLE", PROVIDER_ID).ifPresent(accountRepository::delete);
    }

    @Test
    @DisplayName("다른 요청이 먼저 가입시켜 계정 생성이 유니크 제약에 걸려도 기존 계정으로 로그인하고 계정은 하나다")
    void concurrentSignupConflictLogsInWithExistingAccount() {
        Account existing = accountRepository.saveAndFlush(Account.createOAuth("GOOGLE", PROVIDER_ID, EMAIL, "tx_boundary"));

        OAuth2User result = customOAuth2UserService.loadUser(googleRequest());

        assertThat(((CustomOAuth2User) result).getAccountId()).isEqualTo(existing.getId());
        assertThat(accountRepository.findAll()).filteredOn(account -> PROVIDER_ID.equals(account.getProviderId())).hasSize(1);
    }

    private static OAuth2UserRequest googleRequest() {
        ClientRegistration google = mock(ClientRegistration.class);
        given(google.getRegistrationId()).willReturn("google");
        OAuth2UserRequest request = mock(OAuth2UserRequest.class);
        given(request.getClientRegistration()).willReturn(google);
        return request;
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class Config {

        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("oauth-tx-test");
        }

        // 다른 요청이 첫 조회와 가입 사이에 같은 계정을 만든 순간을 고정한다: 첫 조회만 계정이 없다고 본다.
        @Bean
        AccountQueryService accountQueryService(AccountStore accountStore) {
            AtomicBoolean firstLookup = new AtomicBoolean(true);
            return new AccountQueryService(accountStore) {
                @Override
                public Optional<Account> findByProviderAndProviderId(String provider, String providerId) {
                    return firstLookup.getAndSet(false) ? Optional.empty() : super.findByProviderAndProviderId(provider, providerId);
                }
            };
        }

        // userinfo 엔드포인트를 부르는 대신 Google 사용자 정보를 돌려준다.
        @Bean
        CustomOAuth2UserService customOAuth2UserService(OAuth2AccountService oAuth2AccountService) {
            return new CustomOAuth2UserService(oAuth2AccountService) {
                @Override
                protected OAuth2User loadOAuth2User(OAuth2UserRequest request) {
                    return new DefaultOAuth2User(
                        AuthorityUtils.createAuthorityList("OAUTH2_USER"),
                        Map.of("sub", PROVIDER_ID, "email", EMAIL, "name", "tx"),
                        "sub");
                }
            };
        }
    }
}
