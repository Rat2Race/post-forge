package dev.iamrat.auth.oauth.service;

import dev.iamrat.auth.member.entity.Member;
import dev.iamrat.auth.oauth.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final OAuth2MemberService oAuth2MemberService;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = loadOAuth2User(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = getUserInfo(registrationId, oAuth2User);
        
        String provider = registrationId.toUpperCase();
        Member member = oAuth2MemberService.getOrCreateMember(provider, userInfo);
        
        log.info("OAuth2 로그인: provider={}, providerId={}, email={}, nickname={}",
            member.getProvider(), member.getId(), member.getEmail(), member.getNickname());
        
        return new CustomOAuth2User(
            member.getUserId(),
            member.getNickname(),
            oAuth2User.getAttributes(),
            member.getAuthorities()
        );
    }
    
    protected OAuth2User loadOAuth2User(OAuth2UserRequest oAuth2UserRequest) {
        return super.loadUser(oAuth2UserRequest);
    }
    
    private OAuth2UserInfo getUserInfo(String registrationId, OAuth2User oAuth2User) {
        return switch (registrationId) {
            case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
            case "naver" -> NaverUserInfo.fromOAuth2Attributes(oAuth2User.getAttributes());
            case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        };
    }
}
