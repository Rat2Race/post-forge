package dev.iamrat.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SECURITY_SCHEME = "bearerAuth";
    private static final String INTERNAL_API_KEY_SECURITY_SCHEME = "internalApiKey";

    @Bean
    public OpenAPI postForgeOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("PostForge API")
                .version("0.1.0")
                .description("""
                    PostForge REST API documentation.

                    Public endpoints can be called without authorization. Protected endpoints use a JWT bearer token.
                    Internal crawl endpoints can also be authorized with the X-Internal-Api-Key header.
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
                    .description("Internal service API key for crawler/automation endpoints.")));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
            .group("all")
            .pathsToMatch("/auth/**", "/user/profile/**", "/posts/**", "/files/s3/**", "/ai/**", "/internal/crawl/**")
            .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("auth")
            .pathsToMatch("/auth/**", "/user/profile/**")
            .build();
    }

    @Bean
    public GroupedOpenApi boardApi() {
        return GroupedOpenApi.builder()
            .group("board")
            .pathsToMatch("/posts/**", "/files/s3/**")
            .build();
    }

    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
            .group("ai")
            .pathsToMatch("/ai/**")
            .build();
    }

    @Bean
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
            .group("internal")
            .pathsToMatch("/internal/crawl/**")
            .build();
    }

    @Bean
    public OperationCustomizer securityOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (isInternalEndpoint(handlerMethod)) {
                addSecurityRequirement(operation, JWT_SECURITY_SCHEME);
                addSecurityRequirement(operation, INTERNAL_API_KEY_SECURITY_SCHEME);
                return operation;
            }

            if (hasPreAuthorize(handlerMethod) || isProtectedBySecurityFilter(handlerMethod)) {
                addSecurityRequirement(operation, JWT_SECURITY_SCHEME);
            }

            return operation;
        };
    }

    private void addSecurityRequirement(Operation operation, String schemeName) {
        operation.addSecurityItem(new SecurityRequirement().addList(schemeName));
    }

    private boolean hasPreAuthorize(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PreAuthorize.class) != null
            || AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class) != null;
    }

    private boolean isProtectedBySecurityFilter(HandlerMethod handlerMethod) {
        String packageName = handlerMethod.getBeanType().getPackageName();
        return packageName.startsWith("dev.iamrat.ai")
            || packageName.startsWith("dev.iamrat.document");
    }

    private boolean isInternalEndpoint(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getPackageName().startsWith("dev.iamrat.internal");
    }
}
