package dev.iamrat.ai.support.infrastructure.openai;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "spring.ai.openai")
public class OpenAiProperties {

    @NotBlank
    private String apiKey;
}
