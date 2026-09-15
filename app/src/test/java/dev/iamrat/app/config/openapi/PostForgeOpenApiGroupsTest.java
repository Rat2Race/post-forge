package dev.iamrat.app.config.openapi;

import static dev.iamrat.app.config.openapi.OpenApiConfig.JWT_SECURITY_SCHEME;
import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

class PostForgeOpenApiGroupsTest {
    private final PostForgeOpenApiGroups groups = new PostForgeOpenApiGroups();
    private final OperationCustomizer customizer = groups.securityOperationCustomizer();

    @Test
    @DisplayName("명시적인 JWT 정책 애너테이션이 bearerAuth 요구사항을 추가한다")
    void customize_addsJwtRequirementFromExplicitPolicy() throws Exception {
        Operation operation = customize(ExplicitJwtController.class, "secured");

        assertSecuritySchemes(operation, JWT_SECURITY_SCHEME);
    }

    @Test
    @DisplayName("내부 엔드포인트 정책은 JWT 보안 요구사항을 추가한다")
    void customize_addsJwtRequirementForInternalPolicy() throws Exception {
        Operation operation = customize(InternalController.class, "ingest");

        assertSecuritySchemes(operation, JWT_SECURITY_SCHEME);
    }

    @Test
    @DisplayName("@PreAuthorize는 기존처럼 JWT 요구사항을 추가한다")
    void customize_addsJwtRequirementFromPreAuthorize() throws Exception {
        Operation operation = customize(PreAuthorizedController.class, "secured");

        assertSecuritySchemes(operation, JWT_SECURITY_SCHEME);
    }

    @Test
    @DisplayName("애너테이션과 @PreAuthorize가 겹쳐도 보안 요구사항을 중복 추가하지 않는다")
    void customize_deduplicatesSecurityRequirements() throws Exception {
        Operation operation = customize(AnnotatedAndPreAuthorizedController.class, "secured");

        assertSecuritySchemes(operation, JWT_SECURITY_SCHEME);
        assertThat(operation.getSecurity()).hasSize(1);
    }

    private Operation customize(Class<?> controllerType, String methodName) throws Exception {
        Object controller = controllerType.getDeclaredConstructor().newInstance();
        HandlerMethod handlerMethod = new HandlerMethod(controller, methodName);
        return customizer.customize(new Operation(), handlerMethod);
    }

    private void assertSecuritySchemes(Operation operation, String... schemes) {
        List<SecurityRequirement> security = operation.getSecurity();
        assertThat(security).isNotNull();
        assertThat(security).hasSize(schemes.length);
        for (String scheme : schemes) {
            assertThat(security)
                    .anySatisfy(requirement -> assertThat(requirement).containsKey(scheme));
        }
    }

    @OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
    static class ExplicitJwtController {
        public void secured() {
        }
    }

    @OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
    static class InternalController {
        public void ingest() {
        }
    }

    static class PreAuthorizedController {
        @PreAuthorize("hasRole('USER')")
        public void secured() {
        }
    }

    @OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
    static class AnnotatedAndPreAuthorizedController {
        @PreAuthorize("hasRole('USER')")
        public void secured() {
        }
    }
}
