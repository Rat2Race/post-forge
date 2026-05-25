package dev.iamrat.auth.oauth.infrastructure.external;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.auth.oauth.application.OAuth2AccountService;
import dev.iamrat.auth.oauth.application.OAuth2UserProfile;
import dev.iamrat.auth.security.principal.CustomOAuth2User;
import dev.iamrat.auth.support.error.AuthErrorCode;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {
    
    @Mock
    private OAuth2AccountService oAuth2AccountService;
    
    private CustomOAuth2UserService customOAuth2UserService;
    private OAuth2UserRequest userRequest;
    
    @BeforeEach
    void setUp() {
        customOAuth2UserService = spy(new CustomOAuth2UserService(oAuth2AccountService));
        userRequest = mock(OAuth2UserRequest.class);
    }

    @Test
    @DisplayName("OAuth2 로그인 시 회원을 조회하거나 생성하고 CustomOAuth2User를 반환한다")
    void oauth2Login_supportedProvider_returnsCustomOAuth2User() {
        ClientRegistration googleClient = mock(ClientRegistration.class);
        given(googleClient.getRegistrationId()).willReturn("google");
        given(userRequest.getClientRegistration()).willReturn(googleClient);
        stubOAuth2User(
            Map.of(
                "sub", "google-user-123",
                "name", "홍길동",
                "email", "test@gmail.com"
            ),
            "sub"
        );
        
        Account created = account("user-uuid-123", "tester");
        given(oAuth2AccountService.getOrCreateAccount(eq("GOOGLE"), any())).willReturn(created);
        
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);
        
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        assertThat(((CustomOAuth2User)result).getAccountId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("1");
        
        verify(oAuth2AccountService).getOrCreateAccount(eq("GOOGLE"), any());
    }

    @Test
    @DisplayName("네이버 OAuth 응답은 response 객체를 사용자 정보로 변환해 회원 조회나 생성에 사용한다")
    void oauth2Login_naverProvider_extractsNestedResponse() {
        ClientRegistration naverClient = mock(ClientRegistration.class);
        given(naverClient.getRegistrationId()).willReturn("naver");
        given(userRequest.getClientRegistration()).willReturn(naverClient);
        stubOAuth2User(
            Map.of(
                "resultcode", "00",
                "message", "success",
                "response", Map.of(
                    "id", "naver-user-123",
                    "name", "네이버유저",
                    "email", "naver@example.com"
                )
            ),
            "response"
        );

        Account created = account(
            "naver-account-uuid",
            "네이버유저",
            "NAVER",
            "naver-user-123"
        );
        given(oAuth2AccountService.getOrCreateAccount(eq("NAVER"), any())).willReturn(created);

        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        ArgumentCaptor<OAuth2UserProfile> userProfileCaptor =
            ArgumentCaptor.forClass(OAuth2UserProfile.class);
        verify(oAuth2AccountService).getOrCreateAccount(eq("NAVER"), userProfileCaptor.capture());
        OAuth2UserProfile userProfile = userProfileCaptor.getValue();

        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        assertThat(userProfile).isInstanceOf(NaverUserInfo.class);
        assertThat(userProfile.getId()).isEqualTo("naver-user-123");
        assertThat(userProfile.getName()).isEqualTo("네이버유저");
        assertThat(userProfile.getEmail()).isEqualTo("naver@example.com");
    }
    
    @Test
    @DisplayName("지원하지 않는 소셜 타입이면 표준 오류 코드 기반 OAuth2 인증 예외를 던진다")
    void oauth2Login_unsupportedProvider_throwsOAuth2AuthenticationException() {
        ClientRegistration unsupportedClient = mock(ClientRegistration.class);
        given(unsupportedClient.getRegistrationId()).willReturn("지원안합니다");
        given(userRequest.getClientRegistration()).willReturn(unsupportedClient);
        stubOAuth2User(Map.of("sub", "google-user-123"), "sub");
        
        OAuth2AuthenticationException exception = assertThrows(
            OAuth2AuthenticationException.class,
            () -> customOAuth2UserService.loadUser(userRequest)
        );
        assertOAuth2ErrorCode(exception, AuthErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
    }

    @Test
    @DisplayName("비활성 OAuth 계정이면 표준 오류 코드 기반 OAuth2 인증 예외를 던진다")
    void oauth2Login_inactiveAccount_throwsOAuth2AuthenticationException() {
        ClientRegistration googleClient = mock(ClientRegistration.class);
        given(googleClient.getRegistrationId()).willReturn("google");
        given(userRequest.getClientRegistration()).willReturn(googleClient);
        stubOAuth2User(
            Map.of(
                "sub", "google-user-123",
                "name", "홍길동",
                "email", "test@gmail.com"
            ),
            "sub"
        );

        Account inactiveAccount = account("user-uuid-123", "tester", AccountStatus.SUSPENDED);
        given(oAuth2AccountService.getOrCreateAccount(eq("GOOGLE"), any())).willReturn(inactiveAccount);

        OAuth2AuthenticationException exception = assertThrows(
            OAuth2AuthenticationException.class,
            () -> customOAuth2UserService.loadUser(userRequest)
        );
        assertOAuth2ErrorCode(exception, AuthErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    private void stubOAuth2User(Map<String, Object> attributes, String nameAttributeKey) {
        OAuth2User oAuth2User = new DefaultOAuth2User(
            Collections.emptyList(),
            attributes,
            nameAttributeKey
        );

        willReturn(oAuth2User)
            .given(customOAuth2UserService)
            .loadOAuth2User(any(OAuth2UserRequest.class));
    }

    private void assertOAuth2ErrorCode(
        OAuth2AuthenticationException exception,
        AuthErrorCode expectedErrorCode
    ) {
        assertThat(exception.getError().getErrorCode()).isEqualTo(expectedErrorCode.name());
        assertThat(exception.getMessage()).contains(expectedErrorCode.getMessage());
    }
    
    private Account account(String username, String nickname) {
        return account(username, nickname, AccountStatus.ACTIVE);
    }

    private Account account(String username, String nickname, AccountStatus status) {
        return account(username, nickname, "GOOGLE", "google-user-123", status);
    }

    private Account account(String username, String nickname, String provider, String providerId) {
        return account(username, nickname, provider, providerId, AccountStatus.ACTIVE);
    }

    private Account account(String username, String nickname, String provider, String providerId, AccountStatus status) {
        Account account = Account.builder()
            .id(1L)
            .username(username)
            .nickname(nickname)
            .email(username + "@test.com")
            .provider(provider)
            .providerId(providerId)
            .status(status)
            .build();
        account.addRole(AccountRole.USER);
        return account;
    }
}
