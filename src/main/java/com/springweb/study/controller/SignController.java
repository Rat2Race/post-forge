package com.springweb.study.controller;

import com.springweb.study.dto.sign_in.request.SignInRequest;
import com.springweb.study.dto.sign_in.response.SignInResponse;
import com.springweb.study.dto.sign_up.request.SignUpRequest;
import com.springweb.study.dto.sign_up.response.SignUpResponse;
import com.springweb.study.service.SignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class SignController {
	private final SignService signService;

	@PostMapping("/sign-up")
	public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(signService.regUser(request));
	}

	@PostMapping("/sign-in")
	public ResponseEntity<SignInResponse> signIn(@RequestBody SignInRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(signService.signIn(request));
	}
}