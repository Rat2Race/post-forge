package dev.iamrat.app.config.openapi;

final class PostForgeOpenApiRoutes {
    static final String[] AUTH = {
            "/auth/**",
            "/user/account/**"
    };
    static final String[] BOARD = {
            "/user/profile",
            "/user/profile/**",
            "/posts/**",
            "/api/posts/**",
            "/files/**"
    };
    static final String[] CATALOG = {
            "/api/products/**",
            "/api/admin/products/**",
            "/api/admin/product-match-candidates/**"
    };
    static final String[] SOURCE = {
            "/api/admin/external-api-logs/**",
            "/api/admin/source-policies/**"
    };
    static final String[] PRICE = {
            "/api/products/*/prices",
            "/api/products/price-drops"
    };
    static final String[] AI = {
            "/ai/**"
    };
    static final String[] INGEST = {
            "/ingest/**"
    };
    static final String[] ALL = {
            "/auth/**",
            "/user/account",
            "/user/account/**",
            "/user/profile",
            "/user/profile/**",
            "/posts/**",
            "/api/posts/**",
            "/api/products/**",
            "/api/admin/products/**",
            "/api/admin/product-match-candidates/**",
            "/api/admin/tracked-keywords/**",
            "/api/admin/collection-jobs/**",
            "/api/admin/external-api-logs/**",
            "/api/admin/source-policies/**",
            "/files/**",
            "/ai/**",
            "/ingest/**"
    };

    private PostForgeOpenApiRoutes() {
    }
}
