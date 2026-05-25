package dev.iamrat.auth.email.infrastructure.redis;

final class RedisEmailVerificationKeys {

    private static final String TOKEN_TO_EMAIL_PREFIX = "email_verify_token:";
    private static final String EMAIL_VERIFIED_PREFIX = "email_verified:";

    private RedisEmailVerificationKeys() {
    }

    static String tokenToEmailKey(String token) {
        return TOKEN_TO_EMAIL_PREFIX + token;
    }

    static String emailVerifiedKey(String email) {
        return EMAIL_VERIFIED_PREFIX + email;
    }
}
