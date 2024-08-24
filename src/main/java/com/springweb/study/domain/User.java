package com.springweb.study.domain;

import com.springweb.study.common.RoleType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@Table(name = "USERS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditingFields {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private Long id;

	@Column(name = "PASSWORD", nullable = false)
	private String password;

	@Column(name = "ACCOUNT", scale = 20, nullable = false, unique = true)
	private String account;

	@Column(name = "USERNAME", nullable = false)
	private String username;

	@Enumerated(EnumType.STRING)
	@Column(name = "ROLE", nullable = false)
	private List<RoleType> role;
}
