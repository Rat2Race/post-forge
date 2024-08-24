package com.springweb.study.dto.user.response;

import com.springweb.study.domain.User;

public record UserUpdateResponse(
		boolean result,
		String name
) {
	public static UserUpdateResponse of(boolean result, User user) {
		return new UserUpdateResponse(result, user.getUsername());
	}
}
