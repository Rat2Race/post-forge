package com.springweb.study.security.domain.dto;

public class AuthResponse {

	private final String jwtToken;

	public AuthResponse(String jwtToken) {
		this.jwtToken = jwtToken;
	}

	public String getJwtToken() {
		return jwtToken;
	}
}
