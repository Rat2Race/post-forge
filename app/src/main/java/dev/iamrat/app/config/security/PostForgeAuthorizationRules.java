package dev.iamrat.app.config.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class PostForgeAuthorizationRules {

    private static final String[] PUBLIC = {
            "/",
            "/index.html",
            "/favicon.ico",
            "/images/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**",
            "/test-console",
            "/test-console/**",
            "/api/test-console",
            "/api/test-console/**",
            "/oauth2/**",
            "/login/oauth2/**"
    };

    private static final String[] PUBLIC_POST = {
            "/auth/register",
            "/auth/login",
            "/auth/token/reissue",
            "/auth/oauth2/exchange",
            "/auth/email/send"
    };

    private static final String[] PUBLIC_GET = {
            "/auth/email/verify",
            "/posts",
            "/api/posts",
            "/posts/auto/price-drops",
            "/api/posts/auto/price-drops",
            "/posts/*",
            "/api/posts/*",
            "/posts/*/comments",
            "/api/posts/*/comments",
            "/api/products",
            "/api/products/search",
            "/api/products/*",
            "/api/products/*/prices",
            "/api/products/price-drops",
            "/api/products/categories",
            "/api/products/categories/*",
            "/api/products/*/posts"
    };

    private static final String[] USER_OR_ADMIN = {
            "/auth/logout",
            "/user/account",
            "/user/account/**",
            "/user/profile",
            "/user/profile/**",
            "/posts",
            "/api/posts",
            "/posts/*",
            "/api/posts/*",
            "/posts/*/like",
            "/api/posts/*/like",
            "/posts/*/comments",
            "/api/posts/*/comments",
            "/posts/*/comments/*",
            "/api/posts/*/comments/*",
            "/posts/*/comments/*/like",
            "/api/posts/*/comments/*/like",
            "/files/**",
            "/ai/**",
            "/ingest/**"
    };

    private static final String[] ADMIN = {
            "/admin/**",
            "/api/admin/**"
    };

    public void customize(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requests
    ) {
        requests
            .requestMatchers(PUBLIC).permitAll()
            .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
            .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
            .requestMatchers(USER_OR_ADMIN).hasAnyRole("USER", "ADMIN")
            .requestMatchers(ADMIN).hasRole("ADMIN")
            .anyRequest().denyAll();
    }
}
