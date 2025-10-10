package com.postforge.global.security.dto;

import com.postforge.api.auth.dto.TokenInfo;
import lombok.Builder;

@Builder
public record TokenResponse(
    String grantType,
    String accessToken,
    String refreshToken
) {
    public static TokenInfo toTokenInfo(TokenResponse response) {
        return new TokenInfo(response.grantType(), response.accessToken(), response.refreshToken());
    }
}
