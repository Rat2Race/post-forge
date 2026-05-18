package dev.iamrat.board.integration.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = MockAccountSecurityContextFactory.class)
public @interface WithMockAccount {
    String accountId() default "1";
    String role() default "USER";
}
