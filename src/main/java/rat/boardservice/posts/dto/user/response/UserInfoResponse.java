package rat.boardservice.posts.dto.user.response;

import com.springweb.board.common.RoleType;
import rat.boardservice.posts.domain.User;

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