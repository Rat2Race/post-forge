package dev.iamrat.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public record ChatRequest(
    @NotBlank(message = "메시지를 입력해주세요.")
    String message
) {
}