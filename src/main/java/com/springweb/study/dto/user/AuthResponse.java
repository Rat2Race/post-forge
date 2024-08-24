package com.springweb.study.dto.user;

public class AuthResponse {

	private final String jwtToken;

	public AuthResponse(String jwtToken) {
		this.jwtToken = jwtToken;
	}

	public String getJwtToken() {
		return jwtToken;
	}
}
