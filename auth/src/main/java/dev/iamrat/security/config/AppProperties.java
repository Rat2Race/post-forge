package dev.iamrat.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Cors cors = new Cors();
    private final OAuth2 oauth2 = new OAuth2();
    private final Email email = new Email();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class OAuth2 {
        private String redirectUrl;
    }

    @Getter
    @Setter
    public static class Email {
        private String verificationBaseUrl;
    }
}
