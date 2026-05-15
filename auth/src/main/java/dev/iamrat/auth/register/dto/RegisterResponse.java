package dev.iamrat.auth.register.dto;

public record RegisterResponse(
    Long memberId,
    String message
) {

    public static RegisterResponse of(Long memberId, String message) {
        return new RegisterResponse(memberId, message);
    }
}
