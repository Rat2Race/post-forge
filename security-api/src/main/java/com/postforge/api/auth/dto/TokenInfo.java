package com.postforge.api.auth.dto;

public record TokenInfo(
    String grantType,
    String accessToken,
    String refreshToken
) {
}
