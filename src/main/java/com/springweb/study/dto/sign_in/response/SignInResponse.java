package com.springweb.study.dto.sign_in.response;

import com.springweb.study.common.RoleType;
import com.springweb.study.domain.User;

public record SignInResponse(
		String name,
		RoleType type,
		String token
) {
	public static SignInResponse from(User user, String token) {
		return new SignInResponse(
				user.getUsername(),
				user.getRole(),
				token
		);
	}
}
