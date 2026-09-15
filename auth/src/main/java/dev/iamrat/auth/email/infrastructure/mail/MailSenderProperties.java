package dev.iamrat.auth.email.infrastructure.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailSenderProperties {

    private String username = "noreply@postforge.dev";
}
