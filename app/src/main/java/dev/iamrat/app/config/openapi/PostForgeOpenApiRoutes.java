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
    static final String[] CATALOG = {
            "/api/products/**",
            "/api/admin/products/**",
            "/api/admin/product-match-candidates/**"
    };
    static final String[] PRICE = {
            "/api/products/*/prices",
            "/api/price-checks"
    };
    static final String[] AI = {
            "/api/ai/**"
    };
    static final String[] INGEST = {
            "/api/ingest/**",
            "/api/admin/tracked-keywords/**",
            "/api/admin/collection-jobs/**",
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
            "/api/products/**",
            "/api/price-checks",
            "/api/admin/products/**",
            "/api/admin/product-match-candidates/**",
            "/api/admin/tracked-keywords/**",
            "/api/admin/collection-jobs/**",
            "/api/admin/news-documents/**",
            "/api/admin/launch-news/**",
            "/api/files/**",
            "/api/ai/**",
            "/api/ingest/**"
    };

    private PostForgeOpenApiRoutes() {
    }
}
