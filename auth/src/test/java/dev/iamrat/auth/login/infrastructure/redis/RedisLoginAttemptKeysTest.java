package dev.iamrat.auth.login.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisLoginAttemptKeysTest {

    @Test
    @DisplayName("사용자명 요청 횟수 키는 정규화된 사용자명 단위로 생성한다")
    void usernameRateKey_includesNormalizedUsername() {
        assertThat(RedisLoginAttemptKeys.usernameRateKey("testuser1"))
            .isEqualTo("auth:login:rate:user:testuser1");
    }

    @Test
    @DisplayName("IP 요청 횟수 키는 클라이언트 IP 단위로 생성한다")
    void ipRateKey_includesClientIp() {
        assertThat(RedisLoginAttemptKeys.ipRateKey("127.0.0.1"))
            .isEqualTo("auth:login:rate:ip:127.0.0.1");
    }

    @Test
    @DisplayName("실패 키는 정규화된 사용자명 단위로 생성한다")
    void failureKey_includesNormalizedUsername() {
        assertThat(RedisLoginAttemptKeys.failureKey("testuser1"))
            .isEqualTo("auth:login:fail:testuser1");
    }

    @Test
    @DisplayName("잠금 키는 정규화된 사용자명 단위로 생성한다")
    void lockKey_includesNormalizedUsername() {
        assertThat(RedisLoginAttemptKeys.lockKey("testuser1"))
            .isEqualTo("auth:login:lock:testuser1");
    }
}
