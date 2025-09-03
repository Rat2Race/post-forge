package com.postforge.global.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;

@ConfigurationPropertiesBinding
public record TokenExpirationProperties(
        String accessToken,
        String refreshToken
) {
    public TokenExpirationProperties {
        if (accessToken == null) throw new IllegalArgumentException("accessToken cannot be null");
        if (refreshToken == null) throw new IllegalArgumentException("refreshToken cannot be null");
    }

}
