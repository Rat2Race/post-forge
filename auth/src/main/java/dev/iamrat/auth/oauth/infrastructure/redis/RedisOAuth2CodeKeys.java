package dev.iamrat.auth.oauth.infrastructure.redis;

final class RedisOAuth2CodeKeys {

    private static final String CODE_PREFIX = "oauth2_code:";

    private RedisOAuth2CodeKeys() {
    }

    static String exchangeCodeKey(String code) {
        return CODE_PREFIX + code;
    }
}
