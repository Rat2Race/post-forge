package dev.iamrat.app.config.openapi;

import java.util.Arrays;
import java.util.stream.Stream;

final class PostForgeOpenApiRoutes {
    static final String[] AUTH = {
            "/api/auth/**",
            "/api/user/account",
            "/api/user/account/**",
            "/api/admin/accounts/**"
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
            "/api/ingest/**"
    };
    static final String[] STUDY = {
            "/api/study/**"
    };
    // 그룹을 더할 때 all에서 빠뜨리지 않도록 모듈 그룹을 합쳐 만든다.
    static final String[] ALL = Stream.of(AUTH, BOARD, AI, INGEST, STUDY)
            .flatMap(Arrays::stream)
            .toArray(String[]::new);

    private PostForgeOpenApiRoutes() {
    }
}
