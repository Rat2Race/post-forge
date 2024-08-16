package com.springweb.study.security.service;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

	private final UserRepo userRepo;
	private final PasswordEncoder passwordEncoder;

	public void save(LoginRequestDto request) {
		userRepo.save(User.builder()
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword())).build());
	}

	public void
}
