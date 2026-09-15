package dev.iamrat.auth.token.application;

import lombok.Builder;

@Builder
public record TokenIssueResult(
    String grantType,
    String accessToken,
    String refreshToken
) {
}
