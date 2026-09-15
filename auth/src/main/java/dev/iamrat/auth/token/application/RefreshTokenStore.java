package dev.iamrat.auth.token.application;

public interface RefreshTokenStore {
    void validate(Long accountId, String requestToken);

    void save(Long accountId, String refreshToken);

    void delete(Long accountId);
}
