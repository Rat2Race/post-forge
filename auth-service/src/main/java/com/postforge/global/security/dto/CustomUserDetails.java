package com.postforge.global.security.dto;

import com.postforge.domain.member.entity.Member;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private Long id;
    private String username;
    private String userId;
    private String userPw;
    private Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id, String username, String userId, String userPw, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.userId = userId;
        this.userPw = userPw;
        this.authorities = authorities;
    }

    public static CustomUserDetails of(Member member) {

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getUsername() {
        return this.userId;
    }

    @Override
    public String getPassword() {
        return this.userPw;
    }

}
