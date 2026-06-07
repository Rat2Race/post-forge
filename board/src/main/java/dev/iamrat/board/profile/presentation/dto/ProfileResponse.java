package dev.iamrat.board.profile.presentation.dto;

import dev.iamrat.core.account.AccountProfileDetails;
import java.time.LocalDateTime;
import java.util.List;

public record ProfileResponse(
    Long accountId,
    String username,
    String email,
    String nickname,
    String provider,
    boolean isOAuthUser,
    List<String> roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ProfileResponse from(AccountProfileDetails profile) {
        return new ProfileResponse(
            profile.accountId(),
            profile.username(),
            profile.email(),
            profile.nickname(),
            profile.provider(),
            profile.isOAuthUser(),
            profile.roles(),
            profile.createdAt(),
            profile.updatedAt()
        );
    }
}
