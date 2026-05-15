package dev.iamrat.ingest.error;

import dev.iamrat.core.global.error.ErrorCode;
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
