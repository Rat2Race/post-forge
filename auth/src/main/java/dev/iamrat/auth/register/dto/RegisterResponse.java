package dev.iamrat.auth.register.dto;

public record RegisterResponse(
    Long accountId,
    String message
) {

    public static RegisterResponse of(Long accountId, String message) {
        return new RegisterResponse(accountId, message);
    }
}
