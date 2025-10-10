package com.postforge.domain.member.dto;

import com.postforge.api.auth.dto.LoginRequest;
import jakarta.validation.constraints.NotBlank;

public record CommonLoginRequest(

    @NotBlank(message = "아이디는 필수입니다")
    String id,

    @NotBlank(message = "비밀번호는 필수입니다")
    String pw
) {

    public static CommonLoginRequest from(LoginRequest request) {
        return new CommonLoginRequest(request.id(), request.pw());
    }
}
