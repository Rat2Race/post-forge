package com.postforge.board.posts.exception.handler;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResult {
	private String code;
	private String message;
}