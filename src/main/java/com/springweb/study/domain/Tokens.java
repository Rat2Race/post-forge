package com.springweb.study.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tokens {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	private String refreshToken;

	public Tokens(User user, String refreshToken) {
		this.user = user;
		this.refreshToken = refreshToken;
	}

	public void updateRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public boolean validateRefreshToken(String refreshToken) {
		return this.refreshToken.equals(refreshToken);
	}
}
