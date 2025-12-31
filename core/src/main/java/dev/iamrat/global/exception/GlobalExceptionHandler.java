package dev.iamrat.global.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException: {}", e.getMessage());
        return buildErrorResponse(ErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        log.warn("Invalid path variable: {} cannot be converted to {}",
            e.getValue(), e.getRequiredType().getSimpleName());
        return buildErrorResponse(ErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
