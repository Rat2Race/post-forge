package dev.iamrat.auth.email.application;

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
@ConfigurationProperties(prefix = "auth.email.verification.protection")
public class EmailVerificationProtectionProperties {

    private boolean enabled = true;

    @Min(1)
    private long cooldownSeconds = 10;

    @Min(1)
    private long rateLimitWindowSeconds = 180;

    @Min(1)
    private long emailLimitPerWindow = 5;

    @Min(1)
    private long rateLimitCooldownSeconds = 180;
}
