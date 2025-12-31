package dev.iamrat.security.dto;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CustomUserDetails(
    Long id,
    String userId,
    String userPw,
    Collection<GrantedAuthority> authorities
) implements UserDetails {

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
        return userPw;
    }
}
