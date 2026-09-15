package dev.iamrat.auth.email.application;

public interface EmailVerificationRequestStore {

    EmailVerificationRequestDecision evaluateEmailRequest(
        String normalizedEmail,
        long cooldownSeconds,
        long windowSeconds,
        long limit,
        long lockSeconds
    );
}
