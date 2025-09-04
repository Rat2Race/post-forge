package com.postforge.global.security.dto;

public record TokenReissueRequest(
    String refreshToken
) {

}
