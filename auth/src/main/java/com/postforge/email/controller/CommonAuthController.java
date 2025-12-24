package com.postforge.email.controller;

import com.postforge.member.service.CommonAuthService;
import com.postforge.member.dto.CommonLoginRequest;
import com.postforge.member.dto.CommonRegisterRequest;
import com.postforge.token.dto.TokenReissueRequest;
import com.postforge.token.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class CommonAuthController {

    private final CommonAuthService commonAuthService;

    /** 테스트 **/
    @GetMapping("/security")
    public String operate() {
        return "security";
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody CommonRegisterRequest request) {
        Long memberId = commonAuthService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다. ID: " + memberId);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody CommonLoginRequest request) {
        TokenResponse tokenResponse = commonAuthService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@Valid @RequestBody TokenReissueRequest request) {
        TokenResponse tokenResponse = commonAuthService.reissueToken(request.refreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {
        commonAuthService.logout(userDetails.getUsername());
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}
