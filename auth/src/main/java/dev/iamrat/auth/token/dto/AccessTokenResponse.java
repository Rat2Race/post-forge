package dev.iamrat.auth.token.dto;

import lombok.Builder;

@Builder
public record AccessTokenResponse(
    String grantType,
    String accessToken
) {
}
