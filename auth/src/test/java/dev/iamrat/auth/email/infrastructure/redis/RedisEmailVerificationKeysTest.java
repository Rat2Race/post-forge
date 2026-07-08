package dev.iamrat.auth.email.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisEmailVerificationKeysTest {

    @Test
    @DisplayName("인증 토큰 키는 토큰 단위로 생성한다")
    void tokenToEmailKey_includesToken() {
        assertThat(RedisEmailVerificationKeys.tokenToEmailKey("token"))
            .isEqualTo("email_verify_token:token");
    }

    @Test
    @DisplayName("인증 완료 키는 이메일 단위로 생성한다")
    void emailVerifiedKey_includesEmail() {
        assertThat(RedisEmailVerificationKeys.emailVerifiedKey("tester@test.com"))
            .isEqualTo("email_verified:tester@test.com");
    }

}
