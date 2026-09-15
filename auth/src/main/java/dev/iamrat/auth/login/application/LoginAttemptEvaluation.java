package dev.iamrat.auth.login.application;

public record LoginAttemptEvaluation(
    String normalizedUsername,
    String clientIp,
    long windowSeconds,
    long userLimit,
    long ipLimit
) {
}
