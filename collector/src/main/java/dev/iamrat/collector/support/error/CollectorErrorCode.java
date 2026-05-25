package dev.iamrat.collector.support.error;

import dev.iamrat.core.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CollectorErrorCode implements ErrorCode {

    UNKNOWN_SOURCE(HttpStatus.BAD_REQUEST, "Unknown source");

    private final HttpStatus httpStatus;
    private final String message;
}
