package com.postforge.global.exception.handler.advice;

import com.postforge.global.exception.BadRequestException;
import com.postforge.global.exception.NotFindPage_RestException;
import com.postforge.global.exception.handler.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice(basePackages = "study.controller")
public class RestApiControllerAdvice {

	@ExceptionHandler(NotFindPage_RestException.class)
	public ResponseEntity<ErrorResult> notFindPageRest(NotFindPage_RestException e) {
		log.error("[404 에러]", e);
		ErrorResult errorResult = new ErrorResult("Update-EX", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResult);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResult> badRequest(BadRequestException e) {
		log.error("[401 에러]", e);
		ErrorResult errorResult = new ErrorResult("BadRequest-EX", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}

}