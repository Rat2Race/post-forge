package com.postforge.global.security.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequest(

    @NotBlank(message = "Refresh Token은 필수입니다")
    String refreshToken
) {

}
