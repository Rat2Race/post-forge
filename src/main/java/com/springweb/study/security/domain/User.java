package com.springweb.study.security.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.springweb.study.domain.AuditingFields;
import com.springweb.study.security.service.RoleTypeListDeserializer;
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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MEMBER_ID")
	private Long id;

//	@Column(name = "LOGIN_ID", nullable = false)
//	private String loginId;

	@Column(name = "PASSWORD", nullable = false)
	private String password;

	@Column(name = "EMAIL", nullable = false, unique = true)
	private String email;

	@Column(name = "USERNAME", nullable = false)
	private String username;

	@Enumerated(EnumType.STRING)
	@Column(name = "ROLE", nullable = false)
	@JsonDeserialize(using = RoleTypeListDeserializer.class)
	private List<RoleType> role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus userStatus;
}
