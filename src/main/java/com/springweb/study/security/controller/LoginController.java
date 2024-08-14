package com.springweb.study.security.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@RequiredArgsConstructor
@Controller
public class LoginController {

	@GetMapping("/rootPage")
	public String rootPage() {

		return "rootPage";
	}

	/**
	 * [View] 로그인 페이지를 엽니다.
	 */
	@GetMapping("/login")
	public String login() {
		return "login";
	}

	/**
	 * [Action] 로그인 프로세스를 동작시킨다.
	 */
	@PostMapping("/user/login")
	public ResponseEntity<?> authenticateUser() {
		return ResponseEntity.ok().build();
	}

	/**
	 * [Action] 로그아웃 프로세스를 동작시킨다.
	 */
	@GetMapping("/user/logout")
	public String logout(HttpServletResponse response) {
		// JWT 토큰을 저장하는 쿠키의 값을 삭제
		Cookie jwtCookie = new Cookie("jwt", null);
		jwtCookie.setMaxAge(0);  // 쿠키의 유효기간을 0으로 설정하여 즉시 삭제
		jwtCookie.setPath("/");
		response.addCookie(jwtCookie);

		return "redirect:/login";  // 로그인 페이지로 리다이렉트
	}


}
