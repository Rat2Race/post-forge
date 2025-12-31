package dev.iamrat.oauth.dto;

import dev.iamrat.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public record CustomOAuth2User(
    Member member,
    Map<String, Object> attributes
) implements OAuth2User {
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return member.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getValue()))
            .collect(Collectors.toList());
    }
    
    @Override
    public String getName() {
        return member.getUserId();
    }
    
    public String getUserId() {
        return member.getUserId();
    }
    
    public Long getId() {
        return member.getId();
    }
}
