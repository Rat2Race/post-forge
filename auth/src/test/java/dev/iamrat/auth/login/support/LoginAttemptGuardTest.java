package dev.iamrat.auth.login.support;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LoginAttemptGuardTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginAttemptGuard loginAttemptGuard;

    @BeforeEach
    void setUp() {
        LoginProtectionProperties properties = new LoginProtectionProperties();
        loginAttemptGuard = new LoginAttemptGuard(redisTemplate, properties);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("첫 로그인 요청은 사용자/IP rate limit을 통과한다")
    void guard_firstRequest_allows() {
        given(redisTemplate.hasKey("auth:login:lock:testuser1")).willReturn(false);
        given(valueOperations.increment("auth:login:rate:user:testuser1")).willReturn(1L);
        given(valueOperations.increment("auth:login:rate:ip:127.0.0.1")).willReturn(1L);

        loginAttemptGuard.guard("testuser1", "127.0.0.1");

        verify(redisTemplate).expire("auth:login:rate:user:testuser1", 60L, TimeUnit.SECONDS);
        verify(redisTemplate).expire("auth:login:rate:ip:127.0.0.1", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("잠긴 사용자면 rate limit 증가 전에 429 예외를 던진다")
    void guard_lockedUser_throwsTooManyRequests() {
        given(redisTemplate.hasKey("auth:login:lock:testuser1")).willReturn(true);

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(valueOperations, never()).increment("auth:login:rate:user:testuser1");
    }

    @Test
    @DisplayName("사용자별 분당 로그인 시도 한도를 넘으면 429 예외를 던진다")
    void guard_userRateExceeded_throwsTooManyRequests() {
        given(redisTemplate.hasKey("auth:login:lock:testuser1")).willReturn(false);
        given(valueOperations.increment("auth:login:rate:user:testuser1")).willReturn(11L);

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(valueOperations, never()).increment("auth:login:rate:ip:127.0.0.1");
    }

    @Test
    @DisplayName("실패 횟수가 한도에 도달하면 잠금 키를 만들고 429 예외를 던진다")
    void recordFailure_whenLimitReached_locksUser() {
        given(valueOperations.increment("auth:login:fail:testuser1")).willReturn(5L);

        assertThatThrownBy(() -> loginAttemptGuard.recordFailure("testuser1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(valueOperations).set("auth:login:lock:testuser1", "1", 300L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("로그인 성공 시 실패 카운터와 잠금 키를 제거한다")
    void clearFailure_deletesFailureAndLockKeys() {
        loginAttemptGuard.clearFailure("testuser1");

        verify(redisTemplate).delete(List.of("auth:login:fail:testuser1", "auth:login:lock:testuser1"));
    }

    @Test
    @DisplayName("Redis 장애가 나면 fail-open으로 로그인 가드를 우회한다")
    void guard_whenRedisFails_allowsRequest() {
        given(redisTemplate.hasKey("auth:login:lock:testuser1"))
            .willThrow(new RuntimeException("redis down"));

        loginAttemptGuard.guard("testuser1", "127.0.0.1");

        verify(valueOperations, never()).increment("auth:login:rate:user:testuser1");
    }
}
