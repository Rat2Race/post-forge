package dev.iamrat.auth.login.support;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "auth.login.protection")
public class LoginProtectionProperties {

    private boolean enabled = true;

    @Min(1)
    private long rateLimitWindowSeconds = 60;

    @Min(1)
    private long userLimitPerWindow = 10;

    @Min(1)
    private long ipLimitPerWindow = 30;

    @Min(1)
    private long failureWindowSeconds = 300;

    @Min(1)
    private long failureLimit = 5;

    @Min(1)
    private long lockSeconds = 300;
}
