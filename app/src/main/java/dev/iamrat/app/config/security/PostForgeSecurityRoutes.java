package dev.iamrat.app.config.security;

final class PostForgeSecurityRoutes {
    static final String[] PUBLIC = {
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

    static final String[] PUBLIC_POST = {
            "/auth/register",
            "/auth/login",
            "/auth/token/reissue",
            "/auth/oauth2/exchange",
            "/auth/email/send"
    };

    static final String[] PUBLIC_GET = {
            "/auth/email/verify",
            "/posts",
            "/posts/*",
            "/posts/*/comments"
    };

    static final String[] USER_OR_ADMIN = {
            "/auth/logout",
            "/user/account",
            "/user/account/**",
            "/posts",
            "/posts/*",
            "/posts/*/like",
            "/posts/*/comments",
            "/posts/*/comments/*",
            "/posts/*/comments/*/like",
            "/files/**",
            "/ai/**",
            "/ingest/**"
    };

    static final String[] ADMIN = {
            "/collector/**",
            "/internal/collector/**",
            "/admin/**"
    };

    private PostForgeSecurityRoutes() {
    }
}
