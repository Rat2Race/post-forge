package dev.iamrat.app.config;

final class PostForgeOpenApiRoutes {
    static final String[] ALL = {
            "/auth/**",
            "/user/account",
            "/user/account/**",
            "/posts/**",
            "/files/s3/**",
            "/ai/**",
            "/ingest/**",
            "/collector/**",
            "/internal/collector/**"
    };

    private PostForgeOpenApiRoutes() {
    }
}
