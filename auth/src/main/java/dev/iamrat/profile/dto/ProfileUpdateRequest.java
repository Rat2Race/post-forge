package dev.iamrat.profile.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다")
    String nickname
) {
}
