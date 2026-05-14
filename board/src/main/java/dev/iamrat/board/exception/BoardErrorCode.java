package dev.iamrat.board.exception;

import dev.iamrat.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BoardErrorCode implements ErrorCode {

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"),
    UNAUTHORIZED_POST_ACCESS(HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다"),
    INVALID_COMMENT_PARENT(HttpStatus.BAD_REQUEST, "유효하지 않은 부모 댓글입니다"),
    MAX_COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글의 대댓글은 작성할 수 없습니다"),

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "파일이 비어있습니다"),
    FILE_EXTENSION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 확장자입니다"),
    FILE_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "파일 확장자와 실제 내용이 일치하지 않습니다"),
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다"),
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
