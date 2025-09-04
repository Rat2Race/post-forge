package com.postforge.api.auth.controller;

import com.postforge.api.auth.service.CommonAuthService;
import com.postforge.domain.member.dto.CommonLoginRequest;
import com.postforge.domain.member.dto.CommonRegisterRequest;
import com.postforge.global.security.dto.TokenReissueRequest;
import com.postforge.global.security.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final CommonAuthService commonAuthService;

    /** testing **/
    @GetMapping("/security")
    public String operate() {
        return "security";
    }

    /** 회원가입 **/
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody CommonRegisterRequest request) {
        Long memberId = commonAuthService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다. ID: " + memberId);
    }

    /** 로그인 **/
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody CommonLoginRequest request) {
        TokenResponse tokenResponse = commonAuthService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }

    /** 토큰 재발급 **/
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@Valid @RequestBody TokenReissueRequest request) {
        TokenResponse tokenResponse = commonAuthService.reissueToken(request.refreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    /** 로그아웃 **/
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {
        commonAuthService.logout(userDetails.getUsername());
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}
