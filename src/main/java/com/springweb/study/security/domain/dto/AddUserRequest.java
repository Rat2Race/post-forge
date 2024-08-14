package com.springweb.study.security.domain.dto;

import lombok.Data;

@Data
public class AddUserRequest {
	private String email;
	private String password;
}
