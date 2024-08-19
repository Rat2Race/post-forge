package com.springweb.study.security.service;

import com.springweb.study.security.domain.RoleType;
import com.springweb.study.security.domain.User;
import com.springweb.study.security.domain.UserStatus;
import com.springweb.study.security.domain.dto.AuthRequest;
import com.springweb.study.security.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

	private final UserRepo userRepo;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
		this.userRepo = userRepo;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 새로운 사용자를 등록하는 메서드
	 * @param authRequest 사용자 등록 요청 정보 (이메일, 비밀번호 등)
	 * @return 등록된 사용자 정보
	 */
	public User registerUser(AuthRequest authRequest) {
		if (userRepo.existsByEmail(authRequest.getEmail())) {
			throw new IllegalStateException("Email already exists.");
		}

		String encodedPassword = passwordEncoder.encode(authRequest.getPassword());
		User user = User.builder()
				.username(authRequest.getUsername())
				.email(authRequest.getEmail())
				.password(encodedPassword)
				.build();
		return userRepo.save(user);
	}

	public User authenticateUser(AuthRequest authRequest) {
		User user = userRepo.findByEmail(authRequest.getEmail())
				.orElseThrow(() -> new IllegalStateException("User not found."));
		if (passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
			return user;
		} else {
			throw new IllegalStateException("Invalid password.");
		}
	}

	/**
	 * 사용자 정보 삭제 메서드
	 * @param userId 사용자 ID
	 */
	public void deleteUser(Long userId) {
		userRepo.deleteById(userId);
	}

	/**
	 * 사용자 정보 조회 메서드
	 * @param email 사용자 이메일
	 * @return 사용자 정보
	 */
	public User findUserByEmail(String email) {
		return userRepo.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + email));
	}
}
