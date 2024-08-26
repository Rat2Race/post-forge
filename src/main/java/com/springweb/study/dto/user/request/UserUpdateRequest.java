package com.springweb.study.dto.user.request;

public record UserUpdateRequest(
		String password,
		String newPassword,
		String name
) {
}
