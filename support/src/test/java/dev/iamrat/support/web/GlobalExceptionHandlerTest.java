package dev.iamrat.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.global.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
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

    @Test
    @DisplayName("낙관적 락 충돌은 409로 응답한다")
    void handleOptimisticLockingFailureException_returnsConflict() {
        OptimisticLockingFailureException exception = new OptimisticLockingFailureException("stale account row");

        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailureException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(response.getBody().getMessage()).isEqualTo("동시에 변경된 데이터입니다. 다시 조회 후 시도해주세요");
    }
}
