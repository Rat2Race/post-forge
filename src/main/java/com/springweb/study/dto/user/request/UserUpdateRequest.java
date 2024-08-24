package com.springweb.study.dto.user.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record UserUpdateRequest(
		String password,
		String newPassword,
		String name
) {
}
