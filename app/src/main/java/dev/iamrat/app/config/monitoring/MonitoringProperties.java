package dev.iamrat.app.config.monitoring;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
