package dev.iamrat.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.global.dto.ErrorResponse;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ExceptionResponseHandlerTest {

    private final ExceptionResponseHandler handler = new ExceptionResponseHandler();

    @Test
    @DisplayName("예외 응답 핸들러는 가장 낮은 우선순위의 전역 Advice로 등록된다")
    void handler_isLowestPrecedenceRestControllerAdvice() {
        assertThat(ExceptionResponseHandler.class.isAnnotationPresent(RestControllerAdvice.class)).isTrue();
        assertThat(ExceptionResponseHandler.class.getAnnotation(Order.class).value())
            .isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    @DisplayName("CustomException은 ErrorCode의 상태와 코드를 응답으로 변환한다")
    void handleCustomException_returnsErrorCodeResponse() {
        CustomException exception = new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/posts/1/like");

        ResponseEntity<ErrorResponse> response = handler.handleCustomException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(429);
        assertThat(response.getBody().getError()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(response.getBody().getMessage()).isEqualTo("요청이 너무 많습니다. 잠시 후 다시 시도해주세요");
    }

    @Test
    @DisplayName("요청 검증 실패는 field validation map을 포함한다")
    void handleValidationException_returnsValidationErrorResponse() throws Exception {
        MethodParameter parameter = new MethodParameter(
            ExceptionResponseHandlerTest.class.getDeclaredMethod("validatedMethod", String.class),
            0
        );
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "message", "must not be blank"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getValidation()).containsEntry("message", "must not be blank");
    }

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
    @DisplayName("DB 무결성 제약조건 위반은 409로 응답한다")
    void handleDataIntegrityViolationException_returnsConflict() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("DATA_INTEGRITY_VIOLATION");
        assertThat(response.getBody().getMessage()).isEqualTo("데이터 무결성 제약조건을 위반했습니다");
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

    @SuppressWarnings("unused")
    private void validatedMethod(String message) {
    }
}
