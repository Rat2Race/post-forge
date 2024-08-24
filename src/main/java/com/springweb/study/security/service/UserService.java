package com.springweb.study.security.service;

import com.springweb.study.security.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepo userRepo;
	private final PasswordEncoder passwordEncoder;

}
