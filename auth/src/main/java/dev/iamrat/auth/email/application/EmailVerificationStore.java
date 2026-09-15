package dev.iamrat.auth.email.application;

public interface EmailVerificationStore {
    void saveToken(String token, String email);

    String getEmailAndDeleteToken(String token);

    void markVerified(String email);

    boolean isVerified(String email);

    void removeVerified(String email);
}
