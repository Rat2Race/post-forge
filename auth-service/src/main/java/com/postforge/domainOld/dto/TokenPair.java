package com.postforge.domainOld.dto;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
