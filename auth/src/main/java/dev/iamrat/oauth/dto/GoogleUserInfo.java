package dev.iamrat.oauth.dto;

import java.util.Map;

public record GoogleUserInfo(
    Map<String, Object> attributes
) implements OAuth2UserInfo {
    
    @Override
    public String getId() {
        return (String)  attributes.get("id");
    }
    
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }
    
    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}
