package dev.iamrat.login.dto;

import java.util.Collection;

import dev.iamrat.security.dto.UserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CustomUserDetails(
    String userId,
    String password,
    String nickname,
    Collection<? extends GrantedAuthority> authorities
) implements UserDetails, UserPrincipal {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
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
