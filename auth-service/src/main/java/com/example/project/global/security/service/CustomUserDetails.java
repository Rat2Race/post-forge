package com.example.project.global.security.service;

import com.example.project.domain.member.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    
    private final Member member;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return member.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getValue()))
            .collect(Collectors.toList());
    }
    
    @Override
    public String getPassword() {
        return member.getPassword();
    }
    
    @Override
    public String getUsername() {
        return member.getUsername();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return member.getIsEnabled();
    }
    
    public Long getId() {
        return member.getId();
    }
    
    public String getEmail() {
        return member.getEmail();
    }
}