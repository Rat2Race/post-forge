package dev.iamrat.core.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값 검증 실패"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
