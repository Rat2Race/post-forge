package dev.iamrat.auth.email.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RedisEmailVerificationStoreTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    RedisEmailVerificationStore redisEmailVerificationStore;

    @Test
    @DisplayName("인증 토큰은 email_verify_token prefix와 30분 TTL로 저장한다")
    void saveToken_storesTokenWithTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        redisEmailVerificationStore.saveToken("token", "tester@test.com");

        verify(valueOperations).set("email_verify_token:token", "tester@test.com", 30L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("인증 토큰 조회는 값을 삭제하면서 이메일을 반환한다")
    void getEmailAndDeleteToken_returnsEmail() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete("email_verify_token:token")).willReturn("tester@test.com");

        assertThat(redisEmailVerificationStore.getEmailAndDeleteToken("token")).isEqualTo("tester@test.com");
    }

    @Test
    @DisplayName("인증 완료 상태는 email_verified prefix와 1시간 TTL로 저장한다")
    void markVerified_storesVerifiedFlagWithTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        redisEmailVerificationStore.markVerified("tester@test.com");

        verify(valueOperations).set("email_verified:tester@test.com", "true", 1L, TimeUnit.HOURS);
    }

    @Test
    @DisplayName("인증 완료 상태를 조회한다")
    void isVerified_readsVerifiedFlag() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("email_verified:tester@test.com")).willReturn("true");

        assertThat(redisEmailVerificationStore.isVerified("tester@test.com")).isTrue();
    }

    @Test
    @DisplayName("인증 완료 상태를 삭제한다")
    void removeVerified_deletesVerifiedFlag() {
        redisEmailVerificationStore.removeVerified("tester@test.com");

        verify(redisTemplate).delete("email_verified:tester@test.com");
    }
}
