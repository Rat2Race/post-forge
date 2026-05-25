package dev.iamrat.auth.security.handler;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2RedirectProperties {

    @NotBlank
    private String redirectUrl;
}
