package dev.iamrat.ingest.exception;

import dev.iamrat.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IngestErrorCode implements ErrorCode {

    DOCUMENT_STORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "문서 저장에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
