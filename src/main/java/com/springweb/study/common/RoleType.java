package com.springweb.study.common;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public enum RoleType {
	USER, ADMIN;

	public Collection<? extends GrantedAuthority> toGrantedAuthority() {
		return Collections.singleton(new SimpleGrantedAuthority(this.name()));
	}
}
