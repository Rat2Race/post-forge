package dev.iamrat.token.controller;

import dev.iamrat.token.dto.JwtReissueRequest;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.JwtProvider;
import dev.iamrat.token.service.JwtService;
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
public class JwtController {
    private final JwtProvider jwtProvider;
    
    @PostMapping("/reissue")
    public ResponseEntity<JwtResponse> reissue(@Valid @RequestBody JwtReissueRequest request) {
        return ResponseEntity.ok(jwtProvider.reissueToken(request.refreshToken()));
    }
}
