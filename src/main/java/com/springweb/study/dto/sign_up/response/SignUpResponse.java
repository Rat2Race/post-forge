package com.springweb.study.dto.sign_up.response;

import com.springweb.study.domain.User;

import java.util.UUID;

public record SignUpResponse(
		UUID id,
		String account,
		String name
) {
	public static SignUpResponse from(User user) {
		return new SignUpResponse(
				user.getId(),
				user.getAccount(),
				user.getUsername()
		);
	}
}
