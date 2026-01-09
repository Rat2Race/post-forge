package dev.iamrat.oauth.service;

import dev.iamrat.member.entity.Member;
import dev.iamrat.member.entity.Role;
import dev.iamrat.member.repository.MemberRepository;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.oauth.dto.CustomOAuth2User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private MemberService memberService;
    
    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;
    
    private OAuth2UserRequest mockUserRequest;
    private OAuth2User mockOAuth2User;
    
    @BeforeEach
    void setup() {
        ClientRegistration clientRegistration = ClientRegistration
            .withRegistrationId("google")
            .clientId("test-client-id")
            .clientSecret("test-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/login/oauth2/code/google")
            .scope("profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName("sub")
            .build();
        
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600)
        );
        
        mockUserRequest = new OAuth2UserRequest(clientRegistration, accessToken);
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-user-123");
        attributes.put("name", "홍길동");
        attributes.put("email", "test@gmail.com");
        attributes.put("nickname", "tester");
        
        mockOAuth2User = new DefaultOAuth2User(
            Collections.emptyList(),
            attributes,
            "sub"
        );
    }
    
    @Test
    @DisplayName("신규 사용자 - OAuth2 로그인 시 회원 생성")
    void OAuth2_새로운_회원_생성() {
        when(memberRepository.findByProviderAndProviderId("GOOGLE", "google"))
            .thenReturn(Optional.empty());
        
        Member newMember = Member.builder()
            .userId("user-uuid-123")
            .nickname("tester")
            .email("test@gmail.com")
            .provider("GOOGLE")
            .providerId("google-user-123")
            .build();
        newMember.addRole(Role.USER);
        
        when(memberService.createMember(
            anyString(), anyString(), isNull(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(newMember);
        
        // When
        OAuth2User result = customOAuth2UserService.loadUser(mockUserRequest);
        
        // Then
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.userId()).isEqualTo("user-uuid-123");
        assertThat(customUser.nickname()).isEqualTo("tester");
        
        verify(memberRepository).findByProviderAndProviderId("GOOGLE", "google-user-123");
        verify(memberService).createMember(
            eq("홍길동"), eq("google-user-123"), isNull(),
            eq("test@gmail.com"), anyString(), eq("GOOGLE"), eq("google-user-123")
        );
    }
    
}