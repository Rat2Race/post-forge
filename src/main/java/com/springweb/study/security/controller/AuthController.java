package com.springweb.study.security.controller;

import com.springweb.study.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	@PostMapping("login")
	public ResponseEntity<String> getMemberProfile(
			@Valid @RequestBody LoginRequestDto request
	) {
		String token = this.authService.login(request);
		return ResponseEntity.status(HttpStatus.OK).body(token);
	}
}
