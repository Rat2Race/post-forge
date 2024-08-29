package com.springweb.study.security.impl;

import com.springweb.study.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;


@Getter
public class UserDetailsImpl implements UserDetails {

	private final String account;
	private final String password;
	private final String username;
	private final Collection<? extends GrantedAuthority> authorities;

	public UserDetailsImpl(String account, String password, String username, Collection<? extends GrantedAuthority> authorities) {
		this.account = account;
		this.password = password;
		this.username = username;
		this.authorities = authorities;
	}

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
}
