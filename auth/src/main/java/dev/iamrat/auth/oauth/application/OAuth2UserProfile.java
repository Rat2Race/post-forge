package dev.iamrat.auth.oauth.application;

public interface OAuth2UserProfile {
    String getId();

    String getName();

    String getEmail();
}
