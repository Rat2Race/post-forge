package com.springweb.study.security.service;

import com.springweb.study.security.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;


@Getter
public class UserDetailsImpl implements UserDetails {

	private final User user;
	private final String email;
	private final String password;

	public UserDetailsImpl(User user) {
		this.user = user;
		this.email = user.getEmail();
		this.password = user.getPassword();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		UserRoleEnum role = user.getRole();
		String authority = role.getAuthority();

		SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(authority);
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(simpleGrantedAuthority);

		return authorities;
	}

	@Override
	public String getUsername() {
		return this.email;
	}
}
