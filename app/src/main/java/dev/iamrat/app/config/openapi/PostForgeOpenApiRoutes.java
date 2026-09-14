package dev.iamrat.app.config.openapi;

final class PostForgeOpenApiRoutes {
    static final String[] AUTH = {
            "/api/auth/**",
            "/api/user/account",
            "/api/user/account/**"
    };
    static final String[] BOARD = {
            "/api/posts/**",
            "/api/user/profile",
            "/api/user/profile/**",
            "/api/files/**"
    };
    static final String[] AI = {
            "/api/ai/**"
    };
    static final String[] INGEST = {
            "/api/ingest/**",
            "/api/admin/news-documents/**",
            "/api/admin/launch-news/**"
    };
    static final String[] ALL = {
            "/api/auth/**",
            "/api/user/account",
            "/api/user/account/**",
            "/api/posts/**",
            "/api/user/profile",
            "/api/user/profile/**",
            "/api/admin/news-documents/**",
            "/api/admin/launch-news/**",
            "/api/files/**",
            "/api/ai/**",
            "/api/ingest/**"
    };

    private PostForgeOpenApiRoutes() {
    }
}
