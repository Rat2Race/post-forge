package dev.iamrat.board.profile.presentation.dto;

import dev.iamrat.core.account.AccountProfileDetails;
import java.time.LocalDateTime;
import java.util.List;

public record ProfileResponse(
    Long accountId,
    String username,
    String email,
    String nickname,
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
            profile.roles(),
            profile.createdAt(),
            profile.updatedAt()
        );
    }
}
