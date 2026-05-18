package dev.iamrat.auth.token.dto;

import lombok.Builder;

@Builder
public record JwtResponse(
    String grantType,
    String accessToken,
    String refreshToken
) {
}
