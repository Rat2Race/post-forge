package dev.iamrat.oauth.dto;

import java.util.Map;

public record GoogleUserInfo(
    Map<String, Object> attributes
) implements OAuth2UserInfo {
    
    @Override
    public String getId() {
        return String.valueOf(attributes.get("sub"));
    }
    
    @Override
    public String getEmail() {
        return String.valueOf(attributes.get("email"));
    }
    
    @Override
    public String getName() {
        return String.valueOf(attributes.get("name"));
    }
    
}
