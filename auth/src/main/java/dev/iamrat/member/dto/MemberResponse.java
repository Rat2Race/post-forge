package dev.iamrat.member.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record MemberResponse(
    Long id,
    String userId,
    List<String> roles
) {

}
