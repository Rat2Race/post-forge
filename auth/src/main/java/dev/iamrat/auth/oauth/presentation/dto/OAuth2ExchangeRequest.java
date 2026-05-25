package dev.iamrat.auth.oauth.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuth2ExchangeRequest(
    @NotBlank(message = "OAuth2 authorization code는 필수입니다")
    String code
) {
}
