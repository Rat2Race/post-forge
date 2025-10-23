package com.postforge.domain.member.dto;

public record EmailVerificationResponse(
    String message,
    String email
) {
    public static EmailVerificationResponse of(String message, String email) {
        return new EmailVerificationResponse(message, email);
    }
}
