package rat.boardservice.posts.exception.handler.advice;

import rat.boardservice.posts.exception.BadRequestException;
import rat.boardservice.posts.exception.NotFindPage_RestException;
import rat.boardservice.posts.exception.handler.ErrorResult;
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