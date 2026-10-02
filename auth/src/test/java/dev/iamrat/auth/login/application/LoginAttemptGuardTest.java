package dev.iamrat.auth.login.application;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
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
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
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

    @Test
    @DisplayName("저장소 장애 로그에는 로그인 아이디와 IP를 남기지 않는다")
    void storeFailureLogs_doNotContainUsernameOrIp(CapturedOutput output) {
        given(loginAttemptLimiter.evaluate(any())).willThrow(new RuntimeException("redis down"));
        given(loginAttemptLimiter.recordFailure(any())).willThrow(new RuntimeException("redis down"));
        willThrow(new RuntimeException("redis down")).given(loginAttemptLimiter).clearFailure(any());

        assertThatThrownBy(() -> loginAttemptGuard.guard("leaky-user", "203.0.113.7")).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> loginAttemptGuard.recordFailure("leaky-user")).isInstanceOf(CustomException.class);
        loginAttemptGuard.clearFailure("leaky-user");

        assertThat(output.getAll())
            .contains("로그인 요청 가드 저장소 장애", "로그인 실패 기록 저장소 장애", "실패 기록 초기화 실패")
            .doesNotContain("leaky-user")
            .doesNotContain("203.0.113.7");
    }
}
