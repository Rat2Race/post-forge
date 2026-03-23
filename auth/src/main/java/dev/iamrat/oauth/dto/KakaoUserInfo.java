package dev.iamrat.oauth.dto;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record KakaoUserInfo(
    Map<String, Object> attributes,
    Map<String, Object> kakaoAccount,
    Map<String, Object> profile
) implements OAuth2UserInfo {

    @SuppressWarnings("unchecked")
    public KakaoUserInfo(Map<String, Object> attributes) {
        this(
            attributes,
            attributes.getOrDefault("kakao_account", Collections.emptyMap()) instanceof Map
                ? (Map<String, Object>) attributes.get("kakao_account")
                : Collections.emptyMap(),
            extractProfile(attributes)
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractProfile(Map<String, Object> attributes) {
        Object account = attributes.get("kakao_account");
        if (account instanceof Map<?, ?> accountMap) {
            Object prof = accountMap.get("profile");
            if (prof instanceof Map<?, ?>) {
                return (Map<String, Object>) prof;
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public String getId() {
        return Objects.toString(attributes.get("id"), null);
    }

    @Override
    public String getName() {
        return Objects.toString(profile.get("nickname"), null);
    }

    @Override
    public String getEmail() {
        return Objects.toString(kakaoAccount.get("email"), null);
    }
}
