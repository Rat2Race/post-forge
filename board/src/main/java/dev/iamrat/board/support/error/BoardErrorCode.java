package dev.iamrat.board.support.error;

import dev.iamrat.core.global.error.ErrorCode;
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
    MAX_COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글의 대댓글은 작성할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
