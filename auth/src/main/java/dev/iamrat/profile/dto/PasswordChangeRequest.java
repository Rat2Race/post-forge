package dev.iamrat.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    String currentPassword,

    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, max = 100, message = "비밀번호는 8~100자 사이여야 합니다")
    String newPassword
) {
}
