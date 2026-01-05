package dev.iamrat.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    
    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 20, message = "사용자명은 4-20자여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "사용자명은 영문자와 숫자만 가능합니다")
    String id,

    @NotBlank(message = "비밀번호는 필수입니다")
    String pw
) {
}
