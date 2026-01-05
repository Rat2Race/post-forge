package dev.iamrat.oauth.dto;

import java.util.Map;

public record NaverUserInfo(
    Map<String, Object> attributes
) implements OAuth2UserInfo{
    
    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = (Map<String, Object>) attributes.get("response");
    }
    
    @Override
    public String getId() {
        return (String) attributes.get("id");
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
