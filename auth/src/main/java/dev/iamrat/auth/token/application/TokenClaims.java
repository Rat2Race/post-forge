package dev.iamrat.auth.token.application;

import java.util.List;

public record TokenClaims(
    String subject,
    List<String> roles
) {
    public TokenClaims {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
