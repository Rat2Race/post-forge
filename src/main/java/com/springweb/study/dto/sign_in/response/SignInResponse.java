package com.springweb.study.dto.sign_in.response;

import com.springweb.study.common.RoleType;

public record SignInResponse(
		String name,
		RoleType type,
		String token
) {
}
