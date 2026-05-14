package dev.iamrat.app.config;

import static dev.iamrat.support.openapi.OpenApiConfig.INTERNAL_API_KEY_SECURITY_SCHEME;
import static dev.iamrat.support.openapi.OpenApiConfig.JWT_SECURITY_SCHEME;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
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
            .pathsToMatch("/auth/**", "/user/profile/**", "/posts/**", "/files/s3/**", "/ai/**", "/ingest/**", "/internal/crawl/**")
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
