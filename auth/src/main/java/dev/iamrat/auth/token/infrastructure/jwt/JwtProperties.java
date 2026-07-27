package dev.iamrat.auth.token.infrastructure.jwt;

import dev.iamrat.auth.token.application.TokenLifetimeSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties implements TokenLifetimeSettings {
    @NotBlank
    @Size(min = 32)
    private String secret;

    @NotNull
    @Positive
    private Long accessTokenValidity;

    @NotNull
    @Positive
    private Long refreshTokenValidity;

    @Override
    public long refreshTokenValidityDays() {
        return refreshTokenValidity;
    }
}
