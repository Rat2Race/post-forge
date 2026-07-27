package dev.iamrat.auth.login.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.iamrat.auth.login.application.LoginAttemptDecision;
import dev.iamrat.auth.login.application.LoginAttemptEvaluation;
import dev.iamrat.auth.login.application.LoginFailureDecision;
import dev.iamrat.auth.login.application.LoginFailureRecord;
import dev.iamrat.support.redis.RedisGuardOperations;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RedisLoginAttemptLimiterTest {

    @Mock
    private RedisGuardOperations redisGuardOperations;

    @Test
    @DisplayName("사용자 잠금 키가 있으면 로그인 요청을 제한한다")
    void evaluate_whenLocked_returnsLocked() {
        RedisLoginAttemptLimiter limiter = new RedisLoginAttemptLimiter(redisGuardOperations);
        given(redisGuardOperations.hasKey("auth:login:lock:testuser1")).willReturn(true);

        LoginAttemptDecision result = limiter.evaluate(new LoginAttemptEvaluation(
            "testuser1",
            "127.0.0.1",
            60L,
            10L,
            30L
        ));

        assertThat(result).isEqualTo(LoginAttemptDecision.LOCKED);
        verify(redisGuardOperations, never()).incrementWithExpiry("auth:login:rate:user:testuser1", 60L);
    }

    @Test
    @DisplayName("사용자/IP 요청 횟수가 한도 안이면 허용한다")
    void evaluate_whenWithinLimit_returnsAllowed() {
        RedisLoginAttemptLimiter limiter = new RedisLoginAttemptLimiter(redisGuardOperations);
        given(redisGuardOperations.incrementWithExpiry("auth:login:rate:user:testuser1", 60L)).willReturn(1L);
        given(redisGuardOperations.incrementWithExpiry("auth:login:rate:ip:127.0.0.1", 60L)).willReturn(1L);

        LoginAttemptDecision result = limiter.evaluate(new LoginAttemptEvaluation(
            "testuser1",
            "127.0.0.1",
            60L,
            10L,
            30L
        ));

        assertThat(result).isEqualTo(LoginAttemptDecision.ALLOWED);
        verify(redisGuardOperations).incrementWithExpiry("auth:login:rate:user:testuser1", 60L);
        verify(redisGuardOperations).incrementWithExpiry("auth:login:rate:ip:127.0.0.1", 60L);
    }

    @Test
    @DisplayName("사용자 요청 횟수가 한도를 넘으면 IP 카운터를 증가시키지 않고 제한한다")
    void evaluate_whenUserRateExceeded_returnsUserRateLimited() {
        RedisLoginAttemptLimiter limiter = new RedisLoginAttemptLimiter(redisGuardOperations);
        given(redisGuardOperations.incrementWithExpiry("auth:login:rate:user:testuser1", 60L)).willReturn(11L);

        LoginAttemptDecision result = limiter.evaluate(new LoginAttemptEvaluation(
            "testuser1",
            "127.0.0.1",
            60L,
            10L,
            30L
        ));

        assertThat(result).isEqualTo(LoginAttemptDecision.USER_RATE_LIMITED);
        verify(redisGuardOperations, never()).incrementWithExpiry("auth:login:rate:ip:127.0.0.1", 60L);
    }

    @Test
    @DisplayName("IP 요청 횟수가 한도를 넘으면 제한한다")
    void evaluate_whenIpRateExceeded_returnsIpRateLimited() {
        RedisLoginAttemptLimiter limiter = new RedisLoginAttemptLimiter(redisGuardOperations);
        given(redisGuardOperations.incrementWithExpiry("auth:login:rate:user:testuser1", 60L)).willReturn(1L);
        given(redisGuardOperations.incrementWithExpiry("auth:login:rate:ip:127.0.0.1", 60L)).willReturn(31L);

        LoginAttemptDecision result = limiter.evaluate(new LoginAttemptEvaluation(
            "testuser1",
            "127.0.0.1",
            60L,
            10L,
            30L
        ));

        assertThat(result).isEqualTo(LoginAttemptDecision.IP_RATE_LIMITED);
    }

    @Test
    @DisplayName("IP가 없으면 사용자 요청 횟수만 평가한다")
    void evaluate_withoutIp_skipsIpRate() {
        RedisLoginAttemptLimiter limiter = new RedisLoginAttemptLimiter(redisGuardOperations);
        given(redisGuardOperations.incrementWithExpiry("auth:login:rate:user:testuser1", 60L)).willReturn(1L);

        LoginAttemptDecision result = limiter.evaluate(new LoginAttemptEvaluation(
            "testuser1",
            " ",
            60L,
            10L,
            30L
        ));

        assertThat(result).isEqualTo(LoginAttemptDecision.ALLOWED);
        verify(redisGuardOperations, never()).incrementWithExpiry("auth:login:rate:ip: ", 60L);
    }

    @Test
    @DisplayName("로그인 실패 횟수가 한도 미만이면 기록만 한다")
    void recordFailure_whenBelowLimit_returnsRecorded() {
        RedisLoginAttemptLimiter limiter = new RedisLoginAttemptLimiter(redisGuardOperations);
        given(redisGuardOperations.incrementWithExpiry("auth:login:fail:testuser1", 300L)).willReturn(1L);

        LoginFailureDecision result = limiter.recordFailure(new LoginFailureRecord("testuser1", 300L, 5L, 300L));

        assertThat(result).isEqualTo(LoginFailureDecision.RECORDED);
        verify(redisGuardOperations, never()).mark("auth:login:lock:testuser1", 300L);
    }

    @Test
    @DisplayName("로그인 실패 횟수가 한도에 도달하면 잠금 키를 저장한다")
    void recordFailure_whenLimitReached_returnsLocked() {
        RedisLoginAttemptLimiter limiter = new RedisLoginAttemptLimiter(redisGuardOperations);
        given(redisGuardOperations.incrementWithExpiry("auth:login:fail:testuser1", 300L)).willReturn(5L);

        LoginFailureDecision result = limiter.recordFailure(new LoginFailureRecord("testuser1", 300L, 5L, 300L));

        assertThat(result).isEqualTo(LoginFailureDecision.LOCKED);
        verify(redisGuardOperations).mark("auth:login:lock:testuser1", 300L);
    }

    @Test
    @DisplayName("로그인 성공 시 실패 카운터와 잠금 키를 제거한다")
    void clearFailure_deletesFailureAndLockKeys() {
        RedisLoginAttemptLimiter limiter = new RedisLoginAttemptLimiter(redisGuardOperations);

        limiter.clearFailure("testuser1");

        verify(redisGuardOperations).delete(List.of("auth:login:fail:testuser1", "auth:login:lock:testuser1"));
    }
}
