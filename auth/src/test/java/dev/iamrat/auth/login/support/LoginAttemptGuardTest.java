package dev.iamrat.auth.login.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LoginAttemptGuardTest {

    @Mock
    private LoginAttemptLimiter loginAttemptLimiter;

    private LoginAttemptGuard loginAttemptGuard;

    @BeforeEach
    void setUp() {
        LoginProtectionProperties properties = new LoginProtectionProperties();
        loginAttemptGuard = new LoginAttemptGuard(loginAttemptLimiter, properties);
    }

    @Test
    @DisplayName("첫 로그인 요청은 사용자/IP rate limit을 통과한다")
    void guard_firstRequest_allows() {
        LoginAttemptEvaluation evaluation = new LoginAttemptEvaluation("testuser1", "127.0.0.1", 60L, 10L, 30L);
        given(loginAttemptLimiter.evaluate(evaluation)).willReturn(LoginAttemptDecision.ALLOWED);

        loginAttemptGuard.guard("testuser1", "127.0.0.1");

        verify(loginAttemptLimiter).evaluate(evaluation);
    }

    @Test
    @DisplayName("잠긴 사용자면 rate limit 증가 전에 429 예외를 던진다")
    void guard_lockedUser_throwsTooManyRequests() {
        LoginAttemptEvaluation evaluation = new LoginAttemptEvaluation("testuser1", "127.0.0.1", 60L, 10L, 30L);
        given(loginAttemptLimiter.evaluate(evaluation)).willReturn(LoginAttemptDecision.LOCKED);

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptLimiter).evaluate(evaluation);
    }

    @Test
    @DisplayName("사용자별 분당 로그인 시도 한도를 넘으면 429 예외를 던진다")
    void guard_userRateExceeded_throwsTooManyRequests() {
        LoginAttemptEvaluation evaluation = new LoginAttemptEvaluation("testuser1", "127.0.0.1", 60L, 10L, 30L);
        given(loginAttemptLimiter.evaluate(evaluation)).willReturn(LoginAttemptDecision.USER_RATE_LIMITED);

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptLimiter).evaluate(evaluation);
    }

    @Test
    @DisplayName("IP별 분당 로그인 시도 한도를 넘으면 429 예외를 던진다")
    void guard_ipRateExceeded_throwsTooManyRequests() {
        LoginAttemptEvaluation evaluation = new LoginAttemptEvaluation("testuser1", "127.0.0.1", 60L, 10L, 30L);
        given(loginAttemptLimiter.evaluate(evaluation)).willReturn(LoginAttemptDecision.IP_RATE_LIMITED);

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptLimiter).evaluate(evaluation);
    }

    @Test
    @DisplayName("실패 횟수가 한도에 도달하면 잠금 키를 만들고 429 예외를 던진다")
    void recordFailure_whenLimitReached_locksUser() {
        LoginFailureRecord record = new LoginFailureRecord("testuser1", 300L, 5L, 300L);
        given(loginAttemptLimiter.recordFailure(record)).willReturn(LoginFailureDecision.LOCKED);

        assertThatThrownBy(() -> loginAttemptGuard.recordFailure("testuser1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptLimiter).recordFailure(record);
    }

    @Test
    @DisplayName("로그인 성공 시 실패 카운터와 잠금 키를 제거한다")
    void clearFailure_deletesFailureAndLockKeys() {
        loginAttemptGuard.clearFailure("testuser1");

        verify(loginAttemptLimiter).clearFailure("testuser1");
    }

    @Test
    @DisplayName("Redis 장애가 나면 fail-closed로 로그인 요청을 제한한다")
    void guard_whenRedisFails_throwsTooManyRequests() {
        LoginAttemptEvaluation evaluation = new LoginAttemptEvaluation("testuser1", "127.0.0.1", 60L, 10L, 30L);
        given(loginAttemptLimiter.evaluate(evaluation))
            .willThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptLimiter).evaluate(evaluation);
    }
}
