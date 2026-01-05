package dev.iamrat.oauth.dto;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo{
    
    Map<String, Object> attributes;
    Map<String, Object> kakaoAccount;
    Map<String, Object> profile;
    
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = (Map<String, Object>) kakaoAccount.get("profile");
    }
    
    @Override
    public String getId() {
        return String.valueOf(kakaoAccount.get("id"));
    }
    
    @Override
    public String getEmail() {
        return (String) kakaoAccount.get("email");
    }
    
    @Override
    public String getName() {
        return (String) profile.get("nickname");
    }
}
