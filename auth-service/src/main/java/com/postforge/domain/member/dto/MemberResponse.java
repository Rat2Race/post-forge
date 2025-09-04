package com.postforge.domain.member.dto;

import java.util.List;

public record MemberResponse(
    Long id,
    String name,
    String userId,
    List<String> roles
) {

}
