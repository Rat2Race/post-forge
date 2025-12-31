package dev.iamrat.email.controller;

import dev.iamrat.email.service.EmailVerificationService;
import dev.iamrat.email.dto.EmailVerificationResponse;
import dev.iamrat.email.dto.SendEmailRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send")
    public ResponseEntity<String> sendVerificationEmail(@Valid @RequestBody SendEmailRequest request) {
        emailVerificationService.sendVerificationEmail(request.email());
        return ResponseEntity.ok("인증 메일이 발송되었습니다.");
    }

    @GetMapping("/verify")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(@RequestParam("token") String token) {
        String email = emailVerificationService.verifyEmail(token);
        EmailVerificationResponse response = EmailVerificationResponse.of(
            "이메일 인증이 완료되었습니다.",
            email
        );
        return ResponseEntity.ok(response);
    }
}
