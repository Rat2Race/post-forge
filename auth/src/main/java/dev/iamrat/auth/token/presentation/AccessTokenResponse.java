package dev.iamrat.auth.token.presentation;

import lombok.Builder;

@Builder
public record AccessTokenResponse(
    String grantType,
    String accessToken
) {
}
