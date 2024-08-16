package com.springweb.study.security.domain.dto;

import com.springweb.study.security.domain.RoleType;
import com.springweb.study.security.domain.UserStatus;

import java.time.LocalDateTime;

public record UserDto(
		Long userId,
		String loginId,
		String username,
		String password,
		UserStatus status,
		String email,
		RoleType roleType,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	public static UserDto of(Long userId, String loginId, String username, String password, UserStatus status, String email, RoleType roleType, LocalDateTime createdAt, LocalDateTime updatedAt) {
		return new UserDto(userId, loginId, username, password, status, email, roleType, createdAt, updatedAt);
	}

	public static UserDto of(String loginId) {
		return new UserDto(
				null, loginId, null, null, null, null, null, null, null
		);
	}
}
