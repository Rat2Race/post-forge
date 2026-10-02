package dev.iamrat.study.support.error;

import dev.iamrat.core.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StudyErrorCode implements ErrorCode {

    SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "학습 자료를 찾을 수 없습니다"),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다"),
    EVIDENCE_NOT_IN_SOURCE(HttpStatus.BAD_REQUEST, "근거는 자료에 있는 문장을 그대로 옮겨야 합니다"),
    INVALID_KEY_POINT(HttpStatus.BAD_REQUEST, "자료에 없는 핵심 항목입니다"),
    NOT_DUE_YET(HttpStatus.CONFLICT, "이미 복습한 문제입니다. 다음 복습 때 다시 풀어 주세요"),
    NO_NEW_FOLLOW_UP(HttpStatus.CONFLICT, "이 문제로는 더 만들 꼬리질문이 없습니다. 다른 문제에서 받아 보세요");

    private final HttpStatus httpStatus;
    private final String message;
}
