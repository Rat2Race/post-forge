package com.postforge.board.posts.domain.dto.user.response;

import com.postforge.board.posts.domain.entity.User;

public record UserUpdateResponse(
		boolean result,
		String name
) {
	public static UserUpdateResponse of(boolean result, User user) {
		return new UserUpdateResponse(result, user.getUsername());
	}
}