package com.postforge.auth.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 응답")
public record JwtResponseDto(
        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImF1dGhvcml0aWVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNjI5MjM5MjIyLCJleHAiOjE2MjkzMDI4MjJ9.7")
        String accessToken,

        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImF1dGhvcml0aWVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNjI5MjM5MjIyLCJleHAiOjE2MjkzMDI4MjJ9.7")
        String refreshToken
) {
}
