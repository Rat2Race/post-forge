package com.postforge.global.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildErrorResponse(ErrorCode errorCode) {
        ErrorResponse response = ErrorResponse.builder()
            .status(errorCode.getHttpStatus().value())
            .error(errorCode.name())
            .message(errorCode.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    private ResponseEntity<ErrorResponse> buildValidationErrorResponse(
        ErrorCode errorCode,
        Map<String, String> validation
    ) {
        ErrorResponse response = ErrorResponse.builder()
            .status(errorCode.getHttpStatus().value())
            .error(errorCode.name())
            .message(errorCode.getMessage())
            .validation(validation)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("CustomException: {}", e.getMessage());
        return buildErrorResponse(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return buildValidationErrorResponse(ErrorCode.VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.error("BadCredentialsException: {}", e.getMessage());
        return buildErrorResponse(ErrorCode.INVALID_CREDENTIALS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
