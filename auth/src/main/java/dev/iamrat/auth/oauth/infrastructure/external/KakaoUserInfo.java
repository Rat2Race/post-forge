package dev.iamrat.auth.oauth.infrastructure.external;

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
            extractKakaoAccount(attributes),
            extractProfile(attributes)
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractKakaoAccount(Map<String, Object> attributes) {
        if (attributes == null) {
            return Collections.emptyMap();
        }
        Object account = attributes.get("kakao_account");
        return account instanceof Map<?, ?>
            ? (Map<String, Object>) account
            : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractProfile(Map<String, Object> attributes) {
        Map<String, Object> account = extractKakaoAccount(attributes);
        Object profile = account.get("profile");
        if (profile instanceof Map<?, ?>) {
            return (Map<String, Object>) profile;
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
