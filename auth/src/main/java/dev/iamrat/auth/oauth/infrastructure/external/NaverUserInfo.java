package dev.iamrat.auth.oauth.infrastructure.external;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record NaverUserInfo(
    Map<String, Object> attributes
) implements OAuth2UserInfo {

    public NaverUserInfo {
        attributes = extractResponse(attributes);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractResponse(Map<String, Object> rawAttributes) {
        if (rawAttributes == null) {
            return Collections.emptyMap();
        }
        Object response = rawAttributes.get("response");
        return response instanceof Map<?, ?>
            ? (Map<String, Object>) response
            : Collections.emptyMap();
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
