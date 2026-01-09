package dev.iamrat.oauth.dto;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo{
    
    Map<String, Object> attributes;
    
    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = (Map<String, Object>) attributes.get("response");
    }
    
    @Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }
    
    @Override
    public String getName() {
        return String.valueOf(attributes.get("name"));
    }
    
    @Override
    public String getEmail() {
        return String.valueOf(attributes.get("email"));
    }
    
}
