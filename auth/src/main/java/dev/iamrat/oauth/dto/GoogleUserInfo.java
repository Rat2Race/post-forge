package dev.iamrat.oauth.dto;

import java.util.Map;
import java.util.UUID;

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
    
    @Override
    public String getNickname() {
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        return "user_" + randomId;
    }
}
