package dev.iamrat.support.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

@Configuration
public class OpenApiConfig {

    public static final String JWT_SECURITY_SCHEME = "bearerAuth";
    public static final String INTERNAL_API_KEY_SECURITY_SCHEME = "internalApiKey";

    @Bean
    public OpenAPI postForgeOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("PostForge API")
                .version("0.1.0")
                .description("""
                    PostForge REST API documentation.

                    Public endpoints can be called without authorization. Protected endpoints use a JWT bearer token.
                    Internal collector endpoints can also be authorized with the X-Internal-Api-Key header.
                    """)
                .license(new License().name("Proprietary")))
            .components(new Components()
                .addSecuritySchemes(JWT_SECURITY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT access token returned from POST /auth/login. Use: Bearer {accessToken}."))
                .addSecuritySchemes(INTERNAL_API_KEY_SECURITY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-Internal-Api-Key")
                    .description("Internal service API key for collector/automation endpoints.")));
    }

}
