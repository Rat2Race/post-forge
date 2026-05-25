package dev.iamrat.auth.email.presentation.dto;

import dev.iamrat.auth.support.normalizer.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailRequest(
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email
) {
    public SendEmailRequest {
        email = EmailNormalizer.normalize(email);
    }
}
