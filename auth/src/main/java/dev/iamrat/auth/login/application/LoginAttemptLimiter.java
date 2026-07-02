package dev.iamrat.auth.login.application;

public interface LoginAttemptLimiter {

    LoginAttemptDecision evaluate(LoginAttemptEvaluation evaluation);

    LoginFailureDecision recordFailure(LoginFailureRecord record);

    void clearFailure(String normalizedUsername);
}
