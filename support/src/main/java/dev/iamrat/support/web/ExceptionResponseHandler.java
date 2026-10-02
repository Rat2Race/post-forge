package dev.iamrat.support.web;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.global.error.ErrorCode;
import dev.iamrat.core.global.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class ExceptionResponseHandler {

    private static final Pattern CONSTRAINT_NAME = Pattern.compile("constraint \"([^\"]+)\"");

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
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("CustomException: code={}, status={}, uri={}, message={}",
                errorCode.name(), errorCode.getHttpStatus().value(), request.getRequestURI(), e.getMessage());
        } else {
            log.warn("CustomException: code={}, status={}, uri={}, message={}",
                errorCode.name(), errorCode.getHttpStatus().value(), request.getRequestURI(), e.getMessage());
        }
        return buildErrorResponse(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // 예외 메시지에는 거절된 값(비밀번호 등)이 그대로 실리므로 필드 이름과 위반 코드만 남긴다.
        log.warn("MethodArgumentNotValidException: {}", e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ":" + error.getCode())
            .toList());
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return buildValidationErrorResponse(CommonErrorCode.VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        // 파싱 오류 메시지에는 본문 조각이 실린다. 원인 종류만 남긴다.
        log.warn("HttpMessageNotReadableException: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return buildErrorResponse(CommonErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        // 보낸 값에 토큰 같은 비밀값이 들어올 수 있어 변수 이름과 기대 타입만 남긴다.
        log.warn("Invalid path variable: {} cannot be converted to {}",
            e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        return buildErrorResponse(CommonErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter: {}", e.getParameterName());
        return buildErrorResponse(CommonErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        if ("/favicon.ico".equals(e.getResourcePath()) || "favicon.ico".equals(e.getResourcePath())) {
            log.debug("No favicon resource found");
        } else {
            log.warn("No resource found: {}", e.getResourcePath());
        }
        return buildErrorResponse(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolationException: {}", constraintOf(e));
        return buildErrorResponse(CommonErrorCode.DATA_INTEGRITY_VIOLATION);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
        OptimisticLockingFailureException e
    ) {
        log.warn("OptimisticLockingFailureException: {}", e.getMessage());
        return buildErrorResponse(CommonErrorCode.CONCURRENT_MODIFICATION);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException e) {
        log.error("IOException: {}", e.getMessage(), e);
        return buildErrorResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return buildErrorResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    /** 무결성 위반 메시지에는 충돌한 값(이메일 등)이 실리므로 SQLState와 제약 이름만 꺼낸다. */
    private static String constraintOf(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) {
                Matcher matcher = CONSTRAINT_NAME.matcher(String.valueOf(sql.getMessage()));
                return "sqlState=" + sql.getSQLState() + (matcher.find() ? ", constraint=" + matcher.group(1) : "");
            }
        }
        return e.getClass().getSimpleName();
    }
}
