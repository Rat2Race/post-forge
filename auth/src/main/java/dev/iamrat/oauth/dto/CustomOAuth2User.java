package dev.iamrat.oauth.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public record CustomOAuth2User(
    String userId,
    String nickname,
    Map<String, Object> attributes,
    Collection<GrantedAuthority> authorities
) implements OAuth2User {
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getName() {
        return userId;
    }
    
    public String getNickname() {
        return nickname;
    }
}
