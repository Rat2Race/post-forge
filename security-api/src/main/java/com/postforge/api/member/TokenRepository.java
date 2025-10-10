package com.postforge.api.member;

import java.util.Optional;

public interface TokenRepository {
    void saveRefreshToken(String username, String refreshToken, long ttl);
    Optional<String> getRefreshToken(String username);
    void deleteRefreshToken(String username);
    boolean existsRefreshToken(String username);
}
