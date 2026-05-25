package dev.iamrat.auth.login.infrastructure.redis;

final class RedisLoginAttemptKeys {

    private RedisLoginAttemptKeys() {
    }

    static String usernameRateKey(String normalizedUsername) {
        return "auth:login:rate:user:" + normalizedUsername;
    }

    static String ipRateKey(String clientIp) {
        return "auth:login:rate:ip:" + clientIp;
    }

    static String failureKey(String normalizedUsername) {
        return "auth:login:fail:" + normalizedUsername;
    }

    static String lockKey(String normalizedUsername) {
        return "auth:login:lock:" + normalizedUsername;
    }
}
