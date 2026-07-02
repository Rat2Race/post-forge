package dev.iamrat.auth.email.application;

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
class EmailVerificationRequestGuardTest {

    @Mock
    private EmailVerificationRequestStore emailVerificationRequestStore;

    private EmailVerificationRequestGuard guard;

    @BeforeEach
    void setUp() {
        EmailVerificationProtectionProperties properties = new EmailVerificationProtectionProperties();
        guard = new EmailVerificationRequestGuard(emailVerificationRequestStore, properties);
    }

    @Test
    @DisplayName("이메일 인증 요청 가드가 허용하면 통과한다")
    void guard_firstRequest_allows() {
        given(emailVerificationRequestStore.evaluateEmailRequest("tester@test.com", 10L, 180L, 5L, 180L))
            .willReturn(EmailVerificationRequestDecision.ALLOWED);

        guard.guard(" Tester@Test.COM ");

        verify(emailVerificationRequestStore).evaluateEmailRequest("tester@test.com", 10L, 180L, 5L, 180L);
    }

    @Test
    @DisplayName("180초 이메일 lock 안에 요청하면 429 예외를 던진다")
    void guard_whenEmailLockHit_throwsTooManyRequestsWithoutIncrement() {
        given(emailVerificationRequestStore.evaluateEmailRequest("tester@test.com", 10L, 180L, 5L, 180L))
            .willReturn(EmailVerificationRequestDecision.LOCKED);

        assertThatThrownBy(() -> guard.guard("tester@test.com"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("10초 쿨다운 안의 같은 이메일 인증 요청은 요청 수를 세고 429 예외를 던진다")
    void guard_whenCooldownHit_throwsTooManyRequests() {
        given(emailVerificationRequestStore.evaluateEmailRequest("tester@test.com", 10L, 180L, 5L, 180L))
            .willReturn(EmailVerificationRequestDecision.COOLDOWN);

        assertThatThrownBy(() -> guard.guard("tester@test.com"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("180초 window에서 이메일 요청이 5회를 넘으면 180초 lock을 적용하고 요청 수를 초기화한다")
    void guard_whenEmailRateExceeded_forcesLongCooldown() {
        given(emailVerificationRequestStore.evaluateEmailRequest("tester@test.com", 10L, 180L, 5L, 180L))
            .willReturn(EmailVerificationRequestDecision.RATE_LIMITED);

        assertThatThrownBy(() -> guard.guard("tester@test.com"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("저장소 장애가 나면 fail-closed로 429 예외를 던진다")
    void guard_whenStoreFails_throwsTooManyRequests() {
        given(emailVerificationRequestStore.evaluateEmailRequest("tester@test.com", 10L, 180L, 5L, 180L))
            .willThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> guard.guard("tester@test.com"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }
}
