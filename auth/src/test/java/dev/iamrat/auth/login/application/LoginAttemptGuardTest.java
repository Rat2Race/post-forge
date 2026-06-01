package dev.iamrat.auth.login.application;

import dev.iamrat.auth.login.application.LoginAttemptStore;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LoginAttemptGuardTest {

    @Mock
    private LoginAttemptStore loginAttemptStore;

    private LoginAttemptGuard loginAttemptGuard;

    @BeforeEach
    void setUp() {
        LoginProtectionProperties properties = new LoginProtectionProperties();
        loginAttemptGuard = new LoginAttemptGuard(loginAttemptStore, properties);
    }

    @Test
    @DisplayName("첫 로그인 요청은 사용자/IP rate limit을 통과한다")
    void guard_firstRequest_allows() {
        given(loginAttemptStore.hasLock("testuser1")).willReturn(false);
        given(loginAttemptStore.incrementUserRate("testuser1", 60L)).willReturn(1L);
        given(loginAttemptStore.incrementIpRate("127.0.0.1", 60L)).willReturn(1L);

        loginAttemptGuard.guard("testuser1", "127.0.0.1");

        verify(loginAttemptStore).incrementUserRate("testuser1", 60L);
        verify(loginAttemptStore).incrementIpRate("127.0.0.1", 60L);
    }

    @Test
    @DisplayName("잠긴 사용자면 rate limit 증가 전에 429 예외를 던진다")
    void guard_lockedUser_throwsTooManyRequests() {
        given(loginAttemptStore.hasLock("testuser1")).willReturn(true);

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptStore, never()).incrementUserRate("testuser1", 60L);
    }

    @Test
    @DisplayName("사용자별 분당 로그인 시도 한도를 넘으면 429 예외를 던진다")
    void guard_userRateExceeded_throwsTooManyRequests() {
        given(loginAttemptStore.hasLock("testuser1")).willReturn(false);
        given(loginAttemptStore.incrementUserRate("testuser1", 60L)).willReturn(11L);

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptStore, never()).incrementIpRate("127.0.0.1", 60L);
    }

    @Test
    @DisplayName("실패 횟수가 한도에 도달하면 잠금 키를 만들고 429 예외를 던진다")
    void recordFailure_whenLimitReached_locksUser() {
        given(loginAttemptStore.incrementFailure("testuser1", 300L)).willReturn(5L);

        assertThatThrownBy(() -> loginAttemptGuard.recordFailure("testuser1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptStore).lock("testuser1", 300L);
    }

    @Test
    @DisplayName("로그인 성공 시 실패 카운터와 잠금 키를 제거한다")
    void clearFailure_deletesFailureAndLockKeys() {
        loginAttemptGuard.clearFailure("testuser1");

        verify(loginAttemptStore).clearFailureAndLock("testuser1");
    }

    @Test
    @DisplayName("로그인 성공 후 실패 기록 초기화 장애는 토큰 발급 흐름을 막지 않는다")
    void clearFailure_whenStoreFails_doesNotBlockLoginSuccess() {
        willThrow(new RuntimeException("redis down"))
            .given(loginAttemptStore).clearFailureAndLock("testuser1");

        assertThatCode(() -> loginAttemptGuard.clearFailure("testuser1"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("로그인 가드 저장소 장애가 나면 429 예외로 로그인 시도를 막는다")
    void guard_whenStoreFails_throwsTooManyRequests() {
        given(loginAttemptStore.hasLock("testuser1"))
            .willThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> loginAttemptGuard.guard("testuser1", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptStore, never()).incrementUserRate("testuser1", 60L);
    }

    @Test
    @DisplayName("실패 기록 저장소 장애가 나면 429 예외로 추가 시도를 막는다")
    void recordFailure_whenStoreFails_throwsTooManyRequests() {
        given(loginAttemptStore.incrementFailure("testuser1", 300L))
            .willThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> loginAttemptGuard.recordFailure("testuser1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }
}
