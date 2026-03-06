package dev.iamrat.security.exception;

import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.global.exception.ErrorResponse;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.error("BadCredentialsException: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
            .status(ErrorCode.INVALID_CREDENTIALS.getHttpStatus().value())
            .error(ErrorCode.INVALID_CREDENTIALS.name())
            .message(ErrorCode.INVALID_CREDENTIALS.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.getHttpStatus()).body(response);
    }
}
