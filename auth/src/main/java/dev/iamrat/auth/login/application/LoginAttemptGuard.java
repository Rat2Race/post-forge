package dev.iamrat.auth.login.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptGuard {

    private final LoginAttemptLimiter loginAttemptLimiter;
    private final LoginProtectionProperties properties;

    public void guard(String username, String clientIp) {
        if (!properties.isEnabled() || isBlank(username)) {
            return;
        }

        try {
            String normalizedUsername = normalize(username);
            LoginAttemptDecision decision = loginAttemptLimiter.evaluate(new LoginAttemptEvaluation(
                normalizedUsername,
                clientIp,
                properties.getRateLimitWindowSeconds(),
                properties.getUserLimitPerWindow(),
                properties.getIpLimitPerWindow()
            ));

            if (decision != LoginAttemptDecision.ALLOWED) {
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw failClosed("로그인 요청 가드 저장소 장애", e);
        }
    }

    public void recordFailure(String username) {
        if (!properties.isEnabled() || isBlank(username)) {
            return;
        }

        try {
            String normalizedUsername = normalize(username);
            LoginFailureDecision decision = loginAttemptLimiter.recordFailure(new LoginFailureRecord(
                normalizedUsername,
                properties.getFailureWindowSeconds(),
                properties.getFailureLimit(),
                properties.getLockSeconds()
            ));

            if (decision == LoginFailureDecision.LOCKED) {
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw failClosed("로그인 실패 기록 저장소 장애", e);
        }
    }

    public void clearFailure(String username) {
        if (!properties.isEnabled() || isBlank(username)) {
            return;
        }

        try {
            String normalizedUsername = normalize(username);
            loginAttemptLimiter.clearFailure(normalizedUsername);
        } catch (Exception e) {
            log.warn("로그인 성공 후 실패 기록 초기화 실패: {}", e.getClass().getSimpleName());
        }
    }

    // 로그에는 아이디·IP를 남기지 않는다(docs/policy.md). 장애 종류는 예외 클래스로 구분한다.
    private CustomException failClosed(String message, Exception cause) {
        log.warn("{}: {}", message, cause.getClass().getSimpleName());
        return new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
