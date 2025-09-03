package com.postforge.board.posts.domain.entity;

import com.postforge.board.posts.domain.dto.user.request.UserUpdateRequest;
import jakarta.persistence.*;
import lombok.*;

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

	@Column(name = "USERNAME", nullable = false)
	private String username;

	public void update(UserUpdateRequest request) {
		this.username = request.name();
	}
}