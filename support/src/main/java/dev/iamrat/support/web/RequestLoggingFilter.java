package dev.iamrat.support.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final int MAX_REQUEST_ID_LENGTH = 100;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
            log.info(
                "http_request requestId={} method={} uri={} status={} elapsedMs={}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                String.format(Locale.ROOT, "%.3f", elapsedMs)
            );
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/favicon.ico".equals(uri)
            || uri.startsWith("/actuator/health")
            || uri.startsWith("/actuator/prometheus")
            || uri.startsWith("/swagger-ui")
            || uri.startsWith("/v3/api-docs");
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            return UUID.randomUUID().toString();
        }

        String sanitized = requestId.trim()
            .replace('\r', '_')
            .replace('\n', '_')
            .replace('\t', '_');

        if (sanitized.length() <= MAX_REQUEST_ID_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_REQUEST_ID_LENGTH);
    }
}
