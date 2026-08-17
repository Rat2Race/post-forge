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
            "/oauth2/**",
            "/login/oauth2/**"
    };

    private static final String[] PUBLIC_POST = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/token/reissue",
            "/api/auth/oauth2/exchange",
            "/api/auth/email/send"
    };

    private static final String[] PUBLIC_GET = {
            "/api/auth/email/verify",
            "/api/posts",
            "/api/posts/{postId:\\d+}",
            "/api/posts/{postId:\\d+}/comments",
            "/api/products",
            "/api/products/search",
            "/api/products/{productId:\\d+}",
            "/api/products/{productId:\\d+}/prices",
            "/api/products/categories",
            "/api/products/categories/{categoryId:\\d+}",
            "/api/products/{productId:\\d+}/posts"
    };

    private static final String[] USER_OR_ADMIN = {
            "/api/auth/logout",
            "/api/user/account",
            "/api/user/account/**",
            "/api/user/profile",
            "/api/user/profile/**",
            "/api/posts",
            "/api/posts/*",
            "/api/posts/*/like",
            "/api/posts/*/purchase-vote",
            "/api/posts/*/comments",
            "/api/posts/*/comments/*",
            "/api/posts/*/comments/*/like",
            "/api/files/**",
            "/api/ai/**",
            "/api/price-checks"
    };

    private static final String[] ADMIN = {
            "/api/admin/**",
            "/api/ingest/**"
    };

    public void customize(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requests
    ) {
        requests
            .requestMatchers(PUBLIC).permitAll()
            .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
            .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
            .requestMatchers(ADMIN).hasRole("ADMIN")
            .requestMatchers(USER_OR_ADMIN).hasAnyRole("USER", "ADMIN")
            .anyRequest().denyAll();
    }
}
