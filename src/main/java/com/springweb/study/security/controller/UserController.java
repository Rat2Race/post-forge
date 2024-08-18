package com.springweb.study.security.controller;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.domain.dto.AuthRequest;
import com.springweb.study.security.domain.dto.AuthResponse;
import com.springweb.study.security.service.JwtService;
import com.springweb.study.security.service.UserDetailsImpl;
import com.springweb.study.security.service.UserDetailsServiceImpl;
import com.springweb.study.security.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserService userService;

	// 회원가입 엔드포인트
	@PostMapping("/register")
	public ResponseEntity<User> registerUser(@RequestBody AuthRequest authRequest) {
		User newUser = userService.registerUser(authRequest);
		return ResponseEntity.ok(newUser);
	}

	// 관리자 회원가입 엔드포인트 (예: ADMIN 역할 추가)
//	@PostMapping("/register-admin")
//	public ResponseEntity<User> registerAdminUser(@RequestBody AuthRequest authRequest) {
//		User newAdminUser = userService.registerAdminUser(authRequest);
//		return ResponseEntity.ok(newAdminUser);
//	}

	// 로그인 엔드포인트
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);
		final String jwt = jwtService.generateToken(authentication);

		return ResponseEntity.ok(new AuthResponse(jwt));
	}

	// 로그아웃 엔드포인트 (토큰 블랙리스트 또는 무효화 로직을 추가할 수 있음)
	@PostMapping("/logout")
	public ResponseEntity<String> logout() {
		SecurityContextHolder.clearContext();
		return ResponseEntity.ok("로그아웃 성공");
	}
}
