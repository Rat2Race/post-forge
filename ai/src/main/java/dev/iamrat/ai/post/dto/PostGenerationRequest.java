package dev.iamrat.ai.post.dto;

import jakarta.validation.constraints.NotBlank;

public record PostGenerationRequest(
        @NotBlank String stockCode,
        String corpName,
        boolean publish
) {
}
