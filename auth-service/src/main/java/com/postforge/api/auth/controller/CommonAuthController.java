package com.postforge.api.auth.controller;

import com.postforge.domain.member.dto.CommonLoginRequest;
import com.postforge.domain.member.dto.CommonRegisterRequest;
import com.postforge.global.security.dto.TokenReissueRequest;
import com.postforge.global.security.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class CommonAuthController {

    /** testing **/
    @GetMapping("/security")
    public String operate() {
        return "security";
    }

    /** 회원가입 **/
    @PostMapping("/signup")
    public CommonRegisterRequest signup(@Valid @RequestBody CommonRegisterRequest request) {
        return request;
    }

    /** 로그인 **/
    @PostMapping("/login")
    public CommonLoginRequest login(@Valid @RequestBody CommonLoginRequest request) {
        return request;
    }

    /** 토큰 재발급 **/
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@Valid @RequestBody TokenReissueRequest request) {
        TokenResponse tokenResponse = null;
        return ResponseEntity.ok(tokenResponse);
    }

    /** 로그아웃 **/
    @PostMapping("/logout")
    public String logout() {
        return "logout";
    }
}
