package com.springweb.study.dto.sign_in.request;

public record SignInRequest(
		String account,
		String password
) {
}
