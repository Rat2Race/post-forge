package dev.iamrat.auth.security.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @Valid
    private final Cors cors = new Cors();

    @Valid
    private final OAuth2 oauth2 = new OAuth2();

    @Valid
    private final Email email = new Email();

    @Getter
    @Setter
    public static class Cors {
        @NotEmpty
        private List<@NotBlank String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class OAuth2 {
        @NotBlank
        private String redirectUrl;
    }

    @Getter
    @Setter
    public static class Email {
        @NotBlank
        private String verificationBaseUrl;
    }
}
