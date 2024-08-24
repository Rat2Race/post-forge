package com.springweb.study.dto.user.response;

import com.springweb.study.common.RoleType;
import com.springweb.study.domain.User;

import java.util.UUID;

public record UserInfoResponse(
		UUID id,
		String account,
		String name,
		RoleType type
) {
	public static UserInfoResponse from(User user) {
		return new UserInfoResponse(
				user.getId(),
				user.getAccount(),
				user.getUsername(),
				user.getRole()
		);
	}
}
