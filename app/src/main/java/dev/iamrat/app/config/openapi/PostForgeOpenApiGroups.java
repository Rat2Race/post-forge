package dev.iamrat.app.config.openapi;

import static dev.iamrat.app.config.openapi.OpenApiConfig.JWT_SECURITY_SCHEME;

import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.Arrays;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class PostForgeOpenApiGroups {

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
            .group("all")
            .pathsToMatch(PostForgeOpenApiRoutes.ALL)
            .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("auth")
            .pathsToMatch(PostForgeOpenApiRoutes.AUTH)
            .build();
    }

    @Bean
    public GroupedOpenApi boardApi() {
        return GroupedOpenApi.builder()
            .group("board")
            .pathsToMatch(PostForgeOpenApiRoutes.BOARD)
            .build();
    }

    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
            .group("ai")
            .pathsToMatch(PostForgeOpenApiRoutes.AI)
            .build();
    }

    @Bean
    public GroupedOpenApi ingestApi() {
        return GroupedOpenApi.builder()
            .group("ingest")
            .pathsToMatch(PostForgeOpenApiRoutes.INGEST)
            .build();
    }

    @Bean
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
            .group("internal")
            .pathsToMatch(PostForgeOpenApiRoutes.INTERNAL)
            .build();
    }

    @Bean
    public OperationCustomizer securityOperationCustomizer() {
        return (operation, handlerMethod) -> {
            OpenApiSecurityPolicy policy = findSecurityPolicy(handlerMethod);
            if (policy != null) {
                Arrays.stream(policy.value())
                    .map(this::schemeName)
                    .forEach(schemeName -> addSecurityRequirement(operation, schemeName));
                return operation;
            }

            if (hasPreAuthorize(handlerMethod)) {
                addSecurityRequirement(operation, JWT_SECURITY_SCHEME);
            }

            return operation;
        };
    }

    private void addSecurityRequirement(Operation operation, String schemeName) {
        if (operation.getSecurity() != null
            && operation.getSecurity().stream().anyMatch(requirement -> requirement.containsKey(schemeName))) {
            return;
        }
        operation.addSecurityItem(new SecurityRequirement().addList(schemeName));
    }

    private OpenApiSecurityPolicy findSecurityPolicy(HandlerMethod handlerMethod) {
        OpenApiSecurityPolicy methodPolicy = AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getMethod(),
            OpenApiSecurityPolicy.class
        );
        if (methodPolicy != null) {
            return methodPolicy;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), OpenApiSecurityPolicy.class);
    }

    private boolean hasPreAuthorize(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PreAuthorize.class) != null
            || AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class) != null;
    }

    private String schemeName(OpenApiSecurityPolicy.Scheme scheme) {
        return switch (scheme) {
            case JWT -> JWT_SECURITY_SCHEME;
        };
    }
}
