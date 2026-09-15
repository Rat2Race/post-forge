package dev.iamrat.auth.login.application;

public record LoginFailureRecord(
    String normalizedUsername,
    long windowSeconds,
    long failureLimit,
    long lockSeconds
) {
}
