package dev.iamrat.auth.oauth.application;

public interface OAuth2CodeStore {
    void save(String code, Long accountId);

    String getAndDelete(String code);
}
