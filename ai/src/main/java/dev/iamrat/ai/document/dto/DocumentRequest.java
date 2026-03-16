package dev.iamrat.ai.document.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record DocumentRequest(
    @NotBlank(message = "내용을 입력해주세요.")
    String content,
    String source,
    Map<String, String> metadata
) {
}