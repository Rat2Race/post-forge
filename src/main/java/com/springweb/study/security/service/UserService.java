package com.springweb.study.security.service;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.domain.dto.AddUserRequest;
import com.springweb.study.security.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

	private final UserRepo userRepo;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	public Long save(AddUserRequest request) {
		return userRepo.save(User.builder()
				.email(request.getEmail())
				.password(bCryptPasswordEncoder.encode(request.getPassword()))
				.build()).getId();
	}
}
