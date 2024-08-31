package com.springweb.study.service;

import com.springweb.study.domain.User;
import com.springweb.study.dto.sign_in.request.SignInRequest;
import com.springweb.study.dto.sign_in.response.SignInResponse;
import com.springweb.study.dto.sign_up.request.SignUpRequest;
import com.springweb.study.dto.sign_up.response.SignUpResponse;
import com.springweb.study.dto.user.request.UserUpdateRequest;
import com.springweb.study.dto.user.response.UserUpdateResponse;
import com.springweb.study.repository.UserRepo;
import com.springweb.study.security.jwt.JwtUtils;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignService {

	private final PasswordEncoder encoder;
	private final UserRepo userRepo;
	private final JwtUtils jwtUtils;

	@Transactional
	public SignUpResponse regUser(SignUpRequest request) {
		User user = userRepo.save(User.from(request, encoder));
		return SignUpResponse.from(user);
	}

	@Transactional(readOnly = true)
	public SignInResponse signIn(SignInRequest request) {
		User user = userRepo.findByAccount(request.account())
				.filter(findedUser -> encoder.matches(request.password(), findedUser.getPassword()))
				.orElseThrow(() -> new IllegalArgumentException("not match password"));
		String accessToken = jwtUtils.createAccessToken(user.getUsername());
		String refreshToken = jwtUtils.createRefreshToken();
		return SignInResponse.from(user, accessToken, refreshToken);
	}

	@Transactional
	public UserUpdateResponse updateUser(UUID id, UserUpdateRequest request) {
		return userRepo.findById(id)
				.filter(user -> encoder.matches(request.password(), user.getPassword()))
				.map(user -> {
					user.update(request, encoder);
					return UserUpdateResponse.of(true, user);
				})
				.orElseThrow(() -> new IllegalArgumentException("not match password"));
	}
}
