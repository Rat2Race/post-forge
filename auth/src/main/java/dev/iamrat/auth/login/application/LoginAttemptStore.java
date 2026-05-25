package dev.iamrat.auth.login.application;

public interface LoginAttemptStore {
    boolean hasLock(String normalizedUsername);

    Long incrementUserRate(String normalizedUsername, long windowSeconds);

    Long incrementIpRate(String clientIp, long windowSeconds);

    Long incrementFailure(String normalizedUsername, long windowSeconds);

    void lock(String normalizedUsername, long lockSeconds);

    void clearFailureAndLock(String normalizedUsername);
}
