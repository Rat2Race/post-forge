package com.postforge.api.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

	private final PasswordEncoder passwordEncoder;

	/** 본인 정보 조회 **/
	@GetMapping("/user/profile")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public void getMyProfile() {

	}

	/** 모든 회원 조회 **/
	@GetMapping("/admin/members")
	@PreAuthorize("hasRole('ADMIN')")
	public void getAllMembers() {

	}
}
