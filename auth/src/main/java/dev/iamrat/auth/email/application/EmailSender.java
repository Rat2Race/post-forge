package dev.iamrat.auth.email.application;

public interface EmailSender {
    void sendVerificationEmail(String toEmail, String token);
}
