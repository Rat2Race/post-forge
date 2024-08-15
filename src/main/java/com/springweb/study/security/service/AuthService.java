package com.springweb.study.security.service;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.domain.dto.AddUserRequest;
import com.springweb.study.security.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

	private final UserRepo userRepo;
	private final PasswordEncoder passwordEncoder;

	public void save(AddUserRequest request) {
		userRepo.save(User.builder()
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword()))
			.name(request.getName())
			.number(request.getNumber())
			.role(request.getRole())
			.refreshToken(request.getRefreshToken()).build());
	}
}
