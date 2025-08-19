package com.postforge.board.posts.dto.user.response;

import com.postforge.common.RoleType;
import com.postforge.board.posts.domain.User;

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
				user.getUsername(), // account 대신 username 사용
				user.getUsername(),
				RoleType.USER // 기본값으로 USER 설정
		);
	}
}