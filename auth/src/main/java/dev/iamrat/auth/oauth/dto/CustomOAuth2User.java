package dev.iamrat.auth.oauth.dto;

import dev.iamrat.core.global.security.UserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public record CustomOAuth2User(
    String userId,
    String nickname,
    Map<String, Object> attributes,
    Collection<? extends GrantedAuthority> authorities
) implements OAuth2User, UserPrincipal {
    
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
    
    @Override
    public String getUserId() {
        return userId;
    }
    
    @Override
    public String getNickname() {
        return nickname;
    }
}
