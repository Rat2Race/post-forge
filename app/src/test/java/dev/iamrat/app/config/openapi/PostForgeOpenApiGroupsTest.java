package dev.iamrat.app.config.openapi;

import static dev.iamrat.app.config.openapi.OpenApiConfig.JWT_SECURITY_SCHEME;
import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.unmarked.PackageOnlyAiController;
import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
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
    @DisplayName("app OpenAPI 조립은 전체 API 그룹 경로를 정의한다")
    void allApi_definesAllPublicGroups() {
        assertOpenApiGroup(
            groups.allApi(),
            "all",
            "/auth/**",
            "/user/account",
            "/user/account/**",
            "/posts/**",
            "/files/**",
            "/ai/**",
            "/ingest/**",
            "/collector/**",
            "/internal/collector/**"
        );
    }

    @Test
    @DisplayName("app OpenAPI 조립은 모듈별 API 그룹 경로를 정의한다")
    void moduleApis_defineModuleGroups() {
        assertOpenApiGroup(groups.authApi(), "auth", "/auth/**", "/user/account/**");
        assertOpenApiGroup(groups.boardApi(), "board", "/posts/**", "/files/**");
        assertOpenApiGroup(groups.aiApi(), "ai", "/ai/**");
        assertOpenApiGroup(groups.ingestApi(), "ingest", "/ingest/**", "/internal/collector/**");
        assertOpenApiGroup(groups.internalApi(), "internal", "/internal/collector/**");
    }

    @Test
    @DisplayName("명시적인 JWT 정책 annotation이 bearerAuth requirement를 추가한다")
    void customize_addsJwtRequirementFromExplicitPolicy() throws Exception {
        Operation operation = customize(ExplicitJwtController.class, "secured");

        assertSecuritySchemes(operation, JWT_SECURITY_SCHEME);
    }

    @Test
    @DisplayName("internal endpoint 정책은 JWT security requirement를 추가한다")
    void customize_addsJwtRequirementForInternalPolicy() throws Exception {
        Operation operation = customize(InternalController.class, "ingest");

        assertSecuritySchemes(operation, JWT_SECURITY_SCHEME);
    }

    @Test
    @DisplayName("@PreAuthorize는 기존처럼 JWT requirement를 추가한다")
    void customize_addsJwtRequirementFromPreAuthorize() throws Exception {
        Operation operation = customize(PreAuthorizedController.class, "secured");

        assertSecuritySchemes(operation, JWT_SECURITY_SCHEME);
    }

    @Test
    @DisplayName("package prefix만으로는 security requirement를 추론하지 않는다")
    void customize_doesNotInferSecurityFromPackagePrefixOnly() throws Exception {
        Operation operation = customize(PackageOnlyAiController.class, "unmarked");

        assertThat(operation.getSecurity()).isNull();
    }

    @Test
    @DisplayName("annotation과 @PreAuthorize가 겹쳐도 security requirement를 중복 추가하지 않는다")
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

    private void assertOpenApiGroup(GroupedOpenApi api, String group, String... pathsToMatch) {
        assertThat(api.getGroup()).isEqualTo(group);
        assertThat(api.getPathsToMatch()).containsExactly(pathsToMatch);
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
