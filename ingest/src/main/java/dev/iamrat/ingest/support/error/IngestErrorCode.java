package dev.iamrat.ingest.support.error;

import dev.iamrat.core.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IngestErrorCode implements ErrorCode {

    DOCUMENT_STORE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "문서 저장소를 사용할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
