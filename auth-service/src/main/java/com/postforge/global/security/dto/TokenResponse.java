package com.postforge.global.security.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
    String grantType,
    String accessToken,
    String refreshToken
) {

}
