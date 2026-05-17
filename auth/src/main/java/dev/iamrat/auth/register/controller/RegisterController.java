package dev.iamrat.auth.register.controller;

import dev.iamrat.auth.register.dto.RegisterRequest;
import dev.iamrat.auth.register.dto.RegisterResponse;
import dev.iamrat.auth.register.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/register")
@RequiredArgsConstructor
@Slf4j
public class RegisterController {
    private final RegisterService registerService;
    
    @PostMapping
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        Long accountId = registerService.register(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RegisterResponse.of(accountId, "회원가입이 완료되었습니다."));
    }
}
