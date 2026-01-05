package dev.iamrat.oauth.dto;

import dev.iamrat.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public record CustomOAuth2User(
    String userId,
    String userPw,
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
