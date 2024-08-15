package com.springweb.study.security.controller;

import com.springweb.study.security.domain.dto.AddUserRequest;
import com.springweb.study.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class RegistrationController {

	private final AuthService userService;

	@GetMapping("/signup")
	public String showRegistrationForm() {
		return "signup";
	}

	@PostMapping("/user")
	public String signup(AddUserRequest request){
		userService.save(request); // 회원 가입 메소드 호출
		return "redirect:/login"; // 회원 가입이 완료된 후 로그인 페이지로 이동
	}
}

