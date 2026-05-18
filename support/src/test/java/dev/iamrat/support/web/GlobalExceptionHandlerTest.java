package dev.iamrat.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.global.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("없는 정적 리소스는 500이 아니라 404로 응답한다")
    void handleNoResourceFoundException_returnsNotFound() {
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/favicon.ico");

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
