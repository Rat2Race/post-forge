package dev.iamrat.auth.token.presentation.dto;

import lombok.Builder;

@Builder
public record AccessTokenResponse(
    String grantType,
    String accessToken
) {
}
