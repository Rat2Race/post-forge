package dev.iamrat.auth.support.error;

import dev.iamrat.core.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "활성화되지 않은 계정입니다"),
    INVALID_ACCOUNT_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 계정 상태입니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 회원가입이 완료된 이메일입니다"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다"),

    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "잘못된 비밀번호입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"),

    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
