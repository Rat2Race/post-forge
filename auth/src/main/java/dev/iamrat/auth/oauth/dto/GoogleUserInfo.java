package dev.iamrat.auth.oauth.dto;

import java.util.Map;
import java.util.Objects;

public record GoogleUserInfo(
    Map<String, Object> attributes
) implements OAuth2UserInfo {

    @Override
    public String getId() {
        return Objects.toString(attributes.get("sub"), null);
    }

    @Override
    public String getEmail() {
        return Objects.toString(attributes.get("email"), null);
    }

    @Override
    public String getName() {
        return Objects.toString(attributes.get("name"), null);
    }
}
