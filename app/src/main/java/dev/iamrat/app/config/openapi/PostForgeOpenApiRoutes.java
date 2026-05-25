package dev.iamrat.app.config.openapi;

final class PostForgeOpenApiRoutes {
    static final String[] AUTH = {
            "/auth/**",
            "/user/account/**"
    };
    static final String[] BOARD = {
            "/posts/**",
            "/files/**"
    };
    static final String[] AI = {
            "/ai/**"
    };
    static final String[] INGEST = {
            "/ingest/**",
            "/internal/collector/**"
    };
    static final String[] INTERNAL = {
            "/internal/collector/**"
    };
    static final String[] ALL = {
            "/auth/**",
            "/user/account",
            "/user/account/**",
            "/posts/**",
            "/files/**",
            "/ai/**",
            "/ingest/**",
            "/collector/**",
            "/internal/collector/**"
    };

    private PostForgeOpenApiRoutes() {
    }
}
