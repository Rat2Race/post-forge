package com.postforge.global.security.dto;

public record TokenResponse(
    String grantType,
    String accessToken,
    String refreshToken
) {

}
