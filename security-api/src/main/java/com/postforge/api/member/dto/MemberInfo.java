package com.postforge.api.member.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record MemberInfo(
    Long id,
    String username,
    String userId,
    String userPw,
    Set<Permission> roles,
    Boolean isEnabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
