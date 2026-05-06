package dev.iamrat.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== Common =====
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값 검증 실패"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),

    // ===== Auth Service =====
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 회원가입이 완료된 이메일입니다"),
    DUPLICATE_ID(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다"),

    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "잘못된 비밀번호입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"),

    EMAIL_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 코드를 찾을 수 없습니다"),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다"),
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "이미 인증된 이메일입니다"),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다"),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다"),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다"),

    // ===== Board Service =====
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"),
    UNAUTHORIZED_POST_ACCESS(HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다"),
    INVALID_COMMENT_PARENT(HttpStatus.BAD_REQUEST, "유효하지 않은 부모 댓글입니다"),
    MAX_COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글의 대댓글은 작성할 수 없습니다"),

    // ===== File Service =====
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "파일이 비어있습니다"),
    FILE_EXTENSION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 확장자입니다"),
    FILE_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "파일 확장자와 실제 내용이 일치하지 않습니다"),
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다"),
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽을 수 없습니다"),

    // ===== AI Service =====
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 게시글 생성에 실패했습니다"),
    AI_DOCUMENT_STORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "문서 저장에 실패했습니다"),

    // ===== Crawl Service =====
    CRAWL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다"),
    CRAWL_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 파싱에 실패했습니다"),

    // ===== OAuth =====
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다"),
    OAUTH_USER_INFO_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAuth 사용자 정보를 가져올 수 없습니다"),

    // ===== Profile =====
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    OAUTH_PASSWORD_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
