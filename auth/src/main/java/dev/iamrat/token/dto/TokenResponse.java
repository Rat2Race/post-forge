package dev.iamrat.token.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
    String grantType,
    String accessToken,
    String refreshToken
) {
}
