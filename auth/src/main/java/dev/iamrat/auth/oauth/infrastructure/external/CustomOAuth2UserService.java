package dev.iamrat.auth.oauth.infrastructure.external;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.oauth.application.OAuth2AccountService;
import dev.iamrat.auth.oauth.application.OAuth2UserProfile;
import dev.iamrat.auth.security.infrastructure.principal.AccountAuthorityMapper;
import dev.iamrat.auth.security.infrastructure.principal.CustomOAuth2User;
import dev.iamrat.auth.support.error.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final OAuth2AccountService oAuth2AccountService;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = loadOAuth2User(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserProfile userProfile = getUserProfile(registrationId, oAuth2User);
        
        String provider = registrationId.toUpperCase();
        Account account = oAuth2AccountService.getOrCreateAccount(provider, userProfile);
        if (!account.isActive()) {
            throw oauth2AuthenticationException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        
        log.info("OAuth2 로그인: provider={}, providerId={}, email={}, nickname={}",
            account.getProvider(), account.getId(), account.getEmail(), account.getNickname());
        
        return new CustomOAuth2User(
            account.getId(),
            oAuth2User.getAttributes(),
            AccountAuthorityMapper.toAuthorities(account)
        );
    }
    
    protected OAuth2User loadOAuth2User(OAuth2UserRequest oAuth2UserRequest) {
        return super.loadUser(oAuth2UserRequest);
    }
    
    private OAuth2UserProfile getUserProfile(String registrationId, OAuth2User oAuth2User) {
        return switch (registrationId) {
            case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
            case "naver" -> new NaverUserInfo(oAuth2User.getAttributes());
            case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
            default -> throw oauth2AuthenticationException(AuthErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
        };
    }

    private OAuth2AuthenticationException oauth2AuthenticationException(AuthErrorCode errorCode) {
        OAuth2Error oAuth2Error = new OAuth2Error(errorCode.name(), errorCode.getMessage(), null);
        return new OAuth2AuthenticationException(oAuth2Error, errorCode.getMessage());
    }
}
