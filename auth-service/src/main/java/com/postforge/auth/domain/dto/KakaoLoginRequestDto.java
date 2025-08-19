package com.postforge.auth.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오 로그인 요청")
public record KakaoLoginRequestDto(
        @Schema(description = "카카오 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImF1dGhvcml0aWVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNjI5MjM5MjIyLCJleHAiOjE2MjkzMDI4MjJ9.7")
        String accessToken
) {
}
