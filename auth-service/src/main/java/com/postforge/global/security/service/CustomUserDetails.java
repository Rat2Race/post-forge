package com.postforge.global.security.service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CustomUserDetails(
    Long id,
    String name,
    String userId,
    String userPw,
    Set<String> roles
) implements UserDetails {

    public CustomUserDetails(Long id, String name, String userId, String userPw, Set<String> roles) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.userPw = userPw;
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
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
