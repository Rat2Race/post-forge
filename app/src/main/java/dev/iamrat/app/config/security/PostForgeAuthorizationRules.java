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
            "/images/**"
    };

    private static final String[] PUBLIC_POST = {
            "/auth/register",
            "/auth/login",
            "/auth/token/reissue"
    };

    private static final String[] PUBLIC_GET = {
            "/posts",
            "/api/posts",
            "/posts/*",
            "/api/posts/*",
            "/posts/*/comments",
            "/api/posts/*/comments"
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
            "/api/posts/*/comments/*/like"
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
