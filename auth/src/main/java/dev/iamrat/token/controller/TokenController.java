package dev.iamrat.token.controller;

import dev.iamrat.token.dto.TokenReissueRequest;
import dev.iamrat.token.dto.TokenResponse;
import dev.iamrat.token.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/token")
@RequiredArgsConstructor
@Slf4j
public class TokenController {
    
    private final TokenService tokenService;
    
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@Valid @RequestBody TokenReissueRequest request) {
        TokenResponse tokenResponse = tokenService.reissueToken(request.refreshToken());
        return ResponseEntity.ok(tokenResponse);
    }
}
