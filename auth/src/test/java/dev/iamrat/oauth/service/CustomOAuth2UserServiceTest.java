package dev.iamrat.oauth.service;

import dev.iamrat.member.entity.Member;
import dev.iamrat.member.entity.Role;
import dev.iamrat.oauth.dto.CustomOAuth2User;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private OAuth2MemberService oAuth2MemberService;
    
    private CustomOAuth2UserService customOAuth2UserService;
    private OAuth2UserRequest userRequest;
    private OAuth2User oAuth2User;
    
    @BeforeEach
    void setUp() {
        customOAuth2UserService = spy(new CustomOAuth2UserService(oAuth2MemberService));
        
        userRequest = mock(OAuth2UserRequest.class);
        oAuth2User = new DefaultOAuth2User(
            Collections.emptyList(),
            Map.of(
                "sub", "google-user-123",
                "name", "홍길동",
                "email", "test@gmail.com"
            ),
            "sub"
        );
        
        willReturn(oAuth2User)
            .given(customOAuth2UserService)
            .loadOAuth2User(any(OAuth2UserRequest.class));
    }

    @Test
    @DisplayName("OAuth2 로그인 시 회원을 조회하거나 생성하고 CustomOAuth2User를 반환한다")
    void oauth2Login_supportedProvider_returnsCustomOAuth2User() {
        ClientRegistration googleClient = mock(ClientRegistration.class);
        given(googleClient.getRegistrationId()).willReturn("google");
        given(userRequest.getClientRegistration()).willReturn(googleClient);
        
        Member created = member("user-uuid-123", "tester");
        given(oAuth2MemberService.getOrCreateMember(eq("GOOGLE"), any())).willReturn(created);
        
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);
        
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        assertThat(((CustomOAuth2User)result).userId()).isEqualTo("user-uuid-123");
        assertThat(((CustomOAuth2User)result).nickname()).isEqualTo("tester");
        
        verify(oAuth2MemberService).getOrCreateMember(eq("GOOGLE"), any());
    }
    
    @Test
    @DisplayName("지원하지 않는 소셜 타입이면 OAuth2AuthenticationException을 던진다")
    void oauth2Login_unsupportedProvider_throwsOAuth2AuthenticationException() {
        ClientRegistration unsupportedClient = mock(ClientRegistration.class);
        given(unsupportedClient.getRegistrationId()).willReturn("지원안합니다");
        given(userRequest.getClientRegistration()).willReturn(unsupportedClient);
        
        assertThrows(
            OAuth2AuthenticationException.class,
            () -> customOAuth2UserService.loadUser(userRequest)
        );
    }
    
    private Member member(String userId, String nickname) {
        Member member = Member.builder()
            .userId(userId)
            .nickname(nickname)
            .email(userId + "@test.com")
            .provider("GOOGLE")
            .providerId("google-user-123")
            .build();
        member.addRole(Role.USER);
        return member;
    }
}
