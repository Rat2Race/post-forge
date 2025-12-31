package dev.iamrat.login.controller;

import dev.iamrat.login.service.LoginService;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.login.dto.LoginRequest;
import dev.iamrat.token.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = loginService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {
        loginService.logout(userDetails.getUsername());
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}
