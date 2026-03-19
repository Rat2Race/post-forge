package dev.iamrat.member.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record MemberResponse(
    Long id,
    String userId,
    String nickname,
    String provider,
    boolean isOAuthUser,
    List<String> roles
) {

}
