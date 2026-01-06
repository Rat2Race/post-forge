package dev.iamrat.oauth.service;

import dev.iamrat.member.entity.Member;
import dev.iamrat.member.entity.Role;
import dev.iamrat.member.repository.MemberRepository;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.oauth.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = getUserInfo(registrationId, oAuth2User);
        
        String provider = registrationId.toUpperCase();
        String providerId = userInfo.getId();
        
        Member member = memberRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() ->
                    memberService.createMember(
                        userInfo.getName(),
                        providerId,
                        null,
                        userInfo.getEmail(),
                        generateUniqueNickname(userInfo.getNickname()),
                        provider,
                        providerId
                    )
            );
        //프로필 변경 API 만들어서 닉네임 중복 해결
        
        log.info("OAuth2 로그인: provider={}, providerId={}, email={}", provider, providerId, member.getEmail());
        
        Collection<GrantedAuthority> authorities = member.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getValue()))
            .collect(Collectors.toList());
        
        return new CustomOAuth2User(
            member.getUserId(),
            member.getNickname(),
            oAuth2User.getAttributes(),
            authorities
        );
    }
    
    private String generateUniqueNickname(String nickname) {
        int maxRetries = 100;
        
        for (int i = 0; i < maxRetries; i++) {
            if (!memberRepository.existsByNickname(nickname)) {
                return nickname;
            }
            
            String randomId = UUID.randomUUID().toString().substring(0, 8);
            nickname = "user_" + randomId;
        }
        
        String timestamp = String.valueOf(System.currentTimeMillis() % 1000000);
        String random = UUID.randomUUID().toString().substring(0, 6);
        return "user_" + timestamp + random;
    }
    
    private OAuth2UserInfo getUserInfo(String registrationId, OAuth2User oAuth2User) {
        return switch (registrationId) {
            case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
            case "naver" -> new NaverUserInfo(oAuth2User.getAttributes());
            case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        };
    }
}
