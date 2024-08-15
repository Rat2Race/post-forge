package com.springweb.study.security.domain.dto;

import com.springweb.study.security.domain.UserRoleEnum;
import lombok.Data;

@Data
public class AddUserRequest {
	private String email;
	private String password;
	private String name;
	private String number;
	private UserRoleEnum role;
	private String refreshToken;
}
