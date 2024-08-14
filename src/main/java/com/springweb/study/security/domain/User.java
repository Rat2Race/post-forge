package com.springweb.study.security.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true, length = 30)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String number;

	@Column(nullable = false)
	@Enumerated
	private UserRoleEnum role;

	@Column(length = 1000)
	private String refreshToken;

	public void updateRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public void destroyRefreshToken() {
		this.refreshToken = null;
	}
}
