package com.springweb.study.domain;

import com.springweb.study.common.RoleType;
import com.springweb.study.dto.sign_up.request.SignUpRequest;
import com.springweb.study.dto.user.request.UserUpdateRequest;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@Table(name = "USERS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditingFields {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "PASSWORD", nullable = false)
	private String password;

	@Column(name = "ACCOUNT", scale = 20, nullable = false, unique = true)
	private String account;

	@Column(name = "USERNAME", nullable = false)
	private String username;

	@Enumerated(EnumType.STRING)
	@Column(name = "ROLE", nullable = false)
	private RoleType role;

	public static User from(SignUpRequest request, PasswordEncoder passwordEncoder) {
		return User.builder()
				.account(request.account())
				.password(passwordEncoder.encode(request.password()))
				.username(request.name())
				.role(RoleType.USER)
				.build();
	}

	public void update(UserUpdateRequest request, PasswordEncoder passwordEncoder) {
		this.password = request.newPassword() == null || request.newPassword().isBlank()
				? this.password : passwordEncoder.encode(request.newPassword());
		this.username = request.name();
	}
}
