package rat.boardservice.posts.dto.user.response;

import rat.boardservice.posts.domain.User;

public record UserUpdateResponse(
		boolean result,
		String name
) {
	public static UserUpdateResponse of(boolean result, User user) {
		return new UserUpdateResponse(result, user.getUsername());
	}
}