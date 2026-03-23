package dev.iamrat.oauth.dto;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record NaverUserInfo(
    Map<String, Object> attributes
) implements OAuth2UserInfo {

    @SuppressWarnings("unchecked")
    public static NaverUserInfo fromOAuth2Attributes(Map<String, Object> rawAttributes) {
        Object response = rawAttributes.get("response");
        if (response instanceof Map<?, ?>) {
            return new NaverUserInfo((Map<String, Object>) response);
        }
        return new NaverUserInfo(Collections.emptyMap());
    }

    @Override
    public String getId() {
        return Objects.toString(attributes.get("id"), null);
    }

    @Override
    public String getName() {
        return Objects.toString(attributes.get("name"), null);
    }

    @Override
    public String getEmail() {
        return Objects.toString(attributes.get("email"), null);
    }
}
