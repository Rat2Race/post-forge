package dev.iamrat.auth.login.application;

public enum LoginAttemptDecision {
    ALLOWED,
    LOCKED,
    USER_RATE_LIMITED,
    IP_RATE_LIMITED
}
