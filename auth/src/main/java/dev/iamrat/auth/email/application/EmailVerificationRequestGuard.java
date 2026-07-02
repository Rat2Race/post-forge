package dev.iamrat.auth.email.application;

import dev.iamrat.auth.support.normalizer.EmailNormalizer;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationRequestGuard {

    private final EmailVerificationRequestStore emailVerificationRequestStore;
    private final EmailVerificationProtectionProperties properties;

    public void guard(String email) {
        if (!properties.isEnabled() || isBlank(email)) {
            return;
        }

        try {
            EmailVerificationRequestDecision decision = emailVerificationRequestStore.evaluateEmailRequest(
                EmailNormalizer.normalize(email),
                properties.getCooldownSeconds(),
                properties.getRateLimitWindowSeconds(),
                properties.getEmailLimitPerWindow(),
                properties.getRateLimitCooldownSeconds()
            );

            if (decision != EmailVerificationRequestDecision.ALLOWED) {
                throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("이메일 인증 요청 가드 저장소 장애: email={}", email, e);
            throw new CustomException(CommonErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
