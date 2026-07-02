package dev.iamrat.auth.email.application;

public enum EmailVerificationRequestDecision {
    ALLOWED,
    COOLDOWN,
    LOCKED,
    RATE_LIMITED;

    public static EmailVerificationRequestDecision fromCode(String code) {
        try {
            return EmailVerificationRequestDecision.valueOf(code);
        } catch (RuntimeException e) {
            throw new IllegalStateException("알 수 없는 이메일 인증 요청 결정입니다: " + code, e);
        }
    }
}
