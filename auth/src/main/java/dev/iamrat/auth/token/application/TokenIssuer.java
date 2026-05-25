package dev.iamrat.auth.token.application;

import java.util.Collection;

public interface TokenIssuer {
    String generateAccessToken(Long accountId, Collection<String> authorityNames);

    String generateRefreshToken(Long accountId);

    TokenClaims parse(String token);
}
