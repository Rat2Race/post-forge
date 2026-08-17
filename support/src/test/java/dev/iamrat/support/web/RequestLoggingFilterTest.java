package dev.iamrat.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    @DisplayName("요청 로그는 method, uri, status, elapsedMs만 기록하고 쿼리스트링은 기록하지 않는다")
    void requestLog_includesSafeFieldsOnly(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setQueryString("password=secret");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
            ((MockHttpServletResponse) servletResponse).setStatus(HttpStatus.CREATED.value());

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("request-123");
        assertThat(output)
            .contains("http_request")
            .contains("method=POST")
            .contains("uri=/auth/login")
            .contains("status=201")
            .contains("elapsedMs=")
            .doesNotContain("password=secret");
        assertThat(output.getOut())
            .containsPattern(Pattern.compile("elapsedMs=\\d+\\.\\d{3}"));
    }

    @Test
    @DisplayName("요청 처리 동안 MDC에 requestId를 넣고 처리 후 정리한다")
    void requestLog_putsRequestIdIntoMdcAndClearsAfterward() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "request-777");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringRequest = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
            mdcDuringRequest.set(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY));

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringRequest.get()).isEqualTo("request-777");
        assertThat(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("헬스체크 요청은 공통 요청 로그 대상에서 제외한다")
    void requestLog_skipsHealthCheck(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
            ((MockHttpServletResponse) servletResponse).setStatus(HttpStatus.OK.value());

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNull();
        assertThat(output).doesNotContain("uri=/actuator/health");
    }
}
