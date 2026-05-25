package dev.iamrat.auth.support.web;

import dev.iamrat.support.web.ExceptionResponseHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class TestExceptionResponseHandler extends ExceptionResponseHandler {
}
