package com.springweb.study.security.impl;

import com.springweb.study.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;


public record UserDetailsImpl(
		String account,
		String password,
		String username,
		Collection<?extends GrantedAuthority>authorities
) implements UserDetails {

	public static UserDetailsImpl build(User user) {
		return new UserDetailsImpl(
				user.getAccount(),
				user.getPassword(),
				user.getUsername(),
				user.getRole().toGrantedAuthority()
		);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}
}
