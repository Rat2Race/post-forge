package dev.iamrat.auth.email.infrastructure.mail;

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
@ConfigurationProperties(prefix = "app.email")
public class EmailVerificationProperties {

    @NotBlank
    private String verificationBaseUrl;
}
