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

    @Test
    @DisplayName("인증 메일 발송 쿨다운 키는 이메일 단위로 생성한다")
    void emailSendCooldownKey_includesEmail() {
        assertThat(RedisEmailVerificationKeys.emailSendCooldownKey("tester@test.com"))
            .isEqualTo("email_verify_send:cooldown:email:tester@test.com");
    }

    @Test
    @DisplayName("인증 메일 발송 요청 횟수 키는 이메일 단위로 생성한다")
    void emailSendRateKey_includesEmail() {
        assertThat(RedisEmailVerificationKeys.emailSendRateKey("tester@test.com"))
            .isEqualTo("email_verify_send:rate:email:tester@test.com");
    }

    @Test
    @DisplayName("인증 메일 발송 제한 lock 키는 이메일 단위로 생성한다")
    void emailSendLockKey_includesEmail() {
        assertThat(RedisEmailVerificationKeys.emailSendLockKey("tester@test.com"))
            .isEqualTo("email_verify_send:lock:email:tester@test.com");
    }
}
