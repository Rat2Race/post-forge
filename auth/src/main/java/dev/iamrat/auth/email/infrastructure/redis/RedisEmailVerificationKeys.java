package dev.iamrat.auth.email.infrastructure.redis;

final class RedisEmailVerificationKeys {

    private static final String TOKEN_TO_EMAIL_PREFIX = "email_verify_token:";
    private static final String EMAIL_VERIFIED_PREFIX = "email_verified:";
    private static final String EMAIL_SEND_COOLDOWN_PREFIX = "email_verify_send:cooldown:email:";
    private static final String EMAIL_SEND_RATE_PREFIX = "email_verify_send:rate:email:";
    private static final String EMAIL_SEND_LOCK_PREFIX = "email_verify_send:lock:email:";

    private RedisEmailVerificationKeys() {
    }

    static String tokenToEmailKey(String token) {
        return TOKEN_TO_EMAIL_PREFIX + token;
    }

    static String emailVerifiedKey(String email) {
        return EMAIL_VERIFIED_PREFIX + email;
    }

    static String emailSendCooldownKey(String email) {
        return EMAIL_SEND_COOLDOWN_PREFIX + email;
    }

    static String emailSendRateKey(String email) {
        return EMAIL_SEND_RATE_PREFIX + email;
    }

    static String emailSendLockKey(String email) {
        return EMAIL_SEND_LOCK_PREFIX + email;
    }
}
