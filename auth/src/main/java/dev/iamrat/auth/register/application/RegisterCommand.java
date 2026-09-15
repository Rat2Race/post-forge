package dev.iamrat.auth.register.application;

public record RegisterCommand(
    String username,
    String password,
    String email,
    String nickname
) {
}
