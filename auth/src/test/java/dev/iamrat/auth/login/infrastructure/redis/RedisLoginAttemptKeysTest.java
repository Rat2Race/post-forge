package dev.iamrat.auth.login.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisLoginAttemptKeysTest {

    @Test
    @DisplayName("username rate key는 정규화된 사용자명 단위로 생성한다")
    void usernameRateKey_includesNormalizedUsername() {
        assertThat(RedisLoginAttemptKeys.usernameRateKey("testuser1"))
            .isEqualTo("auth:login:rate:user:testuser1");
    }

    @Test
    @DisplayName("IP rate key는 client IP 단위로 생성한다")
    void ipRateKey_includesClientIp() {
        assertThat(RedisLoginAttemptKeys.ipRateKey("127.0.0.1"))
            .isEqualTo("auth:login:rate:ip:127.0.0.1");
    }

    @Test
    @DisplayName("failure key는 정규화된 사용자명 단위로 생성한다")
    void failureKey_includesNormalizedUsername() {
        assertThat(RedisLoginAttemptKeys.failureKey("testuser1"))
            .isEqualTo("auth:login:fail:testuser1");
    }

    @Test
    @DisplayName("lock key는 정규화된 사용자명 단위로 생성한다")
    void lockKey_includesNormalizedUsername() {
        assertThat(RedisLoginAttemptKeys.lockKey("testuser1"))
            .isEqualTo("auth:login:lock:testuser1");
    }
}
