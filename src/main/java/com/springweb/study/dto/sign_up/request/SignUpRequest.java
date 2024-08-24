package com.springweb.study.dto.sign_up.request;

public record SignUpRequest(
		String account,
		String password,
		String name
) {
}
