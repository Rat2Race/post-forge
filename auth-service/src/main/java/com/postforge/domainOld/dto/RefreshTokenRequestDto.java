package com.postforge.domainOld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 갱신 요청")
public record RefreshTokenRequestDto(
        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImF1dGhvcml0aWVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNjI5MjM5MjIyLCJleHAiOjE2MjkzMDI4MjJ9.7")
        String refreshToken
) {
}
