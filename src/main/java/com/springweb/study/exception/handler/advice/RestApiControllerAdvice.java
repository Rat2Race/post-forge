package com.springweb.study.exception.handler.advice;

import com.springweb.study.exception.BadRequestException;
import com.springweb.study.exception.handler.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "study.controller")
public class RestApiControllerAdvice {

	@ExceptionHandler
	public ResponseEntity<ErrorResult> badRequest(BadRequestException e) {
		log.error("[401 에러]", e);
		ErrorResult errorResult = new ErrorResult("BadRequest-EX", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}
}


