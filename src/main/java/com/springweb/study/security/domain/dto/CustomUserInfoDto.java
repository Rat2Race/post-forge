package com.springweb.study.security.domain.dto;

import com.springweb.study.security.domain.UserRoleEnum;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomUserInfoDto {
	private Long memberId;

	private String email;

	private String name;

	private String password;

	private UserRoleEnum role;
}