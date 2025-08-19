package com.postforge.auth.domain.dto;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
