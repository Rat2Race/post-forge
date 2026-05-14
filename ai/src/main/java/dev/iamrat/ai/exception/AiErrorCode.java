package dev.iamrat.ai.exception;

import dev.iamrat.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiErrorCode implements ErrorCode {

    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 게시글 생성에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
