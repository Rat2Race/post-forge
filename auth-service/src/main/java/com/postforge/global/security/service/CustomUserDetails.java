package com.postforge.global.security.service;

import com.postforge.domain.member.entity.Member;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CustomUserDetails(Member member) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return member.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return member.getUserId();
    }

    @Override
    public String getPassword() {
        return member.getUserPw();
    }

}
