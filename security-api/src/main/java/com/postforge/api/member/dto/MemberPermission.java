package com.postforge.api.member.dto;

import java.util.List;

public record MemberPermission(
    String username,
    List<String> roles
) {

}
