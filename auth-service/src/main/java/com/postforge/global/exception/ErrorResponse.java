package com.postforge.global.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private Map<String, String> validation;
    private LocalDateTime timestamp;
}
