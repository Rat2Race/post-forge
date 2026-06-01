package dev.iamrat.core.account;

import java.time.LocalDateTime;
import java.util.List;

public record AccountProfileDetails(
    Long accountId,
    String username,
    String email,
    String nickname,
    List<String> roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public AccountProfileDetails {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
