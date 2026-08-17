package dev.iamrat.app.config.security;

import dev.iamrat.app.config.monitoring.MetricsConfig;
import dev.iamrat.auth.login.application.CustomUserDetailsService;
import dev.iamrat.auth.security.infrastructure.handler.JwtAccessDeniedHandler;
import dev.iamrat.auth.security.infrastructure.handler.JwtAuthenticationEntryPoint;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.token.application.TokenService;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;

@SpringBootTest(classes = SecurityConfigRegressionTest.TestApp.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "monitoring.username=monitor",
    "monitoring.password=monitor-secret",
    "management.health.defaults.enabled=false",
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class SecurityConfigRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthenticationSuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private AuthenticationFailureHandler oAuth2FailureHandler;

    @BeforeEach
    void setUpAuthenticationDefaults() {
        willAnswer(invocation -> {
            String username = invocation.getArgument(0, String.class);
            UserDetails userDetails = User.withUsername(username)
                .password(passwordEncoder.encode("Test1234!"))
                .roles("USER")
                .build();
            return userDetails;
        }).given(customUserDetailsService).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("게시글 상세 조회는 인증 없이 허용한다")
    void getPostDetail_allowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/posts/1"))
            .andExpect(status().isOk())
            .andExpect(content().string("post-detail"));
    }

    @Test
    @DisplayName("이메일 인증 링크는 잘못된 Bearer 헤더가 있어도 공개 엔드포인트로 처리한다")
    void emailVerification_allowsAnonymousAccessWithInvalidBearerHeader() throws Exception {
        willThrow(new CustomException(AuthErrorCode.INVALID_TOKEN))
            .given(tokenService)
            .resolveAuthentication(anyString());

        mockMvc.perform(get("/api/auth/email/verify")
                .param("token", "email-verification-token")
                .header("Authorization", "Bearer stale-access-token"))
            .andExpect(status().isOk())
            .andExpect(content().string("email-verified"));
    }

    @Test
    @DisplayName("댓글 조회는 인증 없이 허용한다")
    void getComments_allowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/posts/1/comments"))
            .andExpect(status().isOk())
            .andExpect(content().string("comments"));
    }

    @Test
    @DisplayName("상품 상세와 가격 이력 조회는 인증 없이 허용한다")
    void productDetailAndPriceHistory_allowAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(content().string("product-detail"));

        mockMvc.perform(get("/api/products/1/prices"))
            .andExpect(status().isOk())
            .andExpect(content().string("price-history"));
    }

    @Test
    @DisplayName("가격 판정 API는 익명 사용자를 차단한다")
    void priceCheck_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/api/price-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("가격 판정 API는 USER 권한이면 허용한다")
    void priceCheck_allowsUserRole() throws Exception {
        mockMvc.perform(post("/api/price-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().string("price-check"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("제거된 가격 변동 게시 경로는 wildcard 허용에 걸리지 않는다")
    void removedPriceRoutes_areDenied() throws Exception {
        mockMvc.perform(get(removedRoute("/api/products")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get(removedRoute("/api/posts/auto")))
            .andExpect(status().isForbidden());
    }

    private static String removedRoute(String prefix) {
        return prefix + "/price-" + "drops";
    }

    @Test
    @DisplayName("AuthenticationManager는 재귀 없이 사용자 자격 증명을 인증한다")
    void authenticationManager_authenticatesWithoutRecursion() {
        UsernamePasswordAuthenticationToken authenticationToken =
            UsernamePasswordAuthenticationToken.unauthenticated("testuser1", "Test1234!");

        var authentication = authenticationManager.authenticate(authenticationToken);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("testuser1");
    }

    @Test
    @DisplayName("계정 조회는 익명 사용자를 차단한다")
    void getAccount_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/user/account"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("계정 조회는 USER 권한이면 허용한다")
    void getAccount_allowsUserRole() throws Exception {
        mockMvc.perform(get("/api/user/account"))
            .andExpect(status().isOk())
            .andExpect(content().string("account"));
    }

    @Test
    @DisplayName("파일 API는 익명 사용자를 차단한다")
    void fileApi_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/files/presigned-url"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("파일 API는 USER 권한이면 허용한다")
    void fileApi_allowsUserRole() throws Exception {
        mockMvc.perform(get("/api/files/presigned-url"))
            .andExpect(status().isOk())
            .andExpect(content().string("file"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("파일 API S3 경로도 USER 권한이면 허용한다")
    void fileApi_s3Path_allowsUserRole() throws Exception {
        mockMvc.perform(get("/api/files/s3/presigned-url"))
            .andExpect(status().isOk())
            .andExpect(content().string("file"));
    }

    @Test
    @DisplayName("게시글 생성은 익명 사용자를 차단한다")
    void createPost_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("게시글 생성은 USER 권한이면 허용한다")
    void createPost_allowsUserRole() throws Exception {
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().string("created"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("좋아요는 ADMIN만으로는 허용하지 않는다")
    void likePost_rejectsAdminRole() throws Exception {
        mockMvc.perform(post("/api/posts/1/like"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("구매 판단 투표는 익명 사용자를 차단한다")
    void purchaseVote_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(put("/api/posts/1/purchase-vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("구매 판단 투표는 USER 권한이면 허용한다")
    void purchaseVote_allowsUserRole() throws Exception {
        mockMvc.perform(put("/api/posts/1/purchase-vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().string("purchase-voted"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("게시글 삭제는 ADMIN 권한이면 허용한다")
    void deletePost_allowsAdminRole() throws Exception {
        mockMvc.perform(delete("/api/posts/1"))
            .andExpect(status().isOk())
            .andExpect(content().string("deleted"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("댓글 수정은 USER 권한이면 허용한다")
    void updateComment_allowsUserRole() throws Exception {
        mockMvc.perform(put("/api/posts/1/comments/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().string("comment-updated"));
    }

    @Test
    @DisplayName("AI API는 익명 사용자를 차단한다")
    void aiApi_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/ai/ping"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ingest API는 익명 사용자를 차단한다")
    void ingestApi_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/ingest/ping"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Ingest API는 USER 권한을 차단한다")
    void ingestApi_rejectsUserRole() throws Exception {
        mockMvc.perform(get("/api/ingest/ping"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Ingest API는 ADMIN 권한이면 허용한다")
    void ingestApi_allowsAdminRole() throws Exception {
        mockMvc.perform(get("/api/ingest/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("ingest"));
    }

    @Test
    @DisplayName("상품 수집 관리자 경로는 익명 사용자를 차단한다")
    void adminProductCollection_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/api/admin/collection-jobs/manual"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("상품 수집 관리자 경로는 USER 권한을 차단한다")
    void adminProductCollection_rejectsUserRole() throws Exception {
        mockMvc.perform(post("/api/admin/collection-jobs/manual"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 수집 관리자 경로는 ADMIN 권한이면 허용한다")
    void adminProductCollection_allowsAdminRole() throws Exception {
        mockMvc.perform(post("/api/admin/collection-jobs/manual"))
            .andExpect(status().isOk())
            .andExpect(content().string("manual-collected"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("출시 뉴스 관리자 경로는 USER 권한을 차단한다")
    void adminLaunchNews_rejectsUserRole() throws Exception {
        mockMvc.perform(post("/api/admin/launch-news/manual"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("출시 뉴스 관리자 경로는 ADMIN 권한이면 허용한다")
    void adminLaunchNews_allowsAdminRole() throws Exception {
        mockMvc.perform(post("/api/admin/launch-news/manual"))
            .andExpect(status().isOk())
            .andExpect(content().string("launch-news-posted"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("명시되지 않은 경로는 인증된 사용자도 차단한다")
    void undeclaredRoute_deniesAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/undeclared-route"))
            .andExpect(status().isForbidden());
    }

    @EnableAutoConfiguration
    @Import({
        MetricsConfig.class,
        SecurityConfig.class,
        ActuatorSecurityConfig.class,
        PasswordEncoderConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        DummyPostController.class,
        DummyCommentController.class,
        DummyAccountController.class,
        DummyFileController.class,
        DummyAiController.class,
        DummyIngestController.class,
        DummyProductController.class,
        DummyPriceCheckController.class,
        DummyEmailVerificationController.class,
        DummyAdminProductCollectionController.class,
        DummyAdminLaunchNewsController.class
    })
    static class TestApp {

        @Bean
        PostForgeAuthorizationRules postForgeAuthorizationRules() {
            return new PostForgeAuthorizationRules();
        }

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration registration = ClientRegistration.withRegistrationId("test")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("profile")
                .authorizationUri("https://example.com/oauth2/authorize")
                .tokenUri("https://example.com/oauth2/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("id")
                .clientName("Test OAuth")
                .build();

            return new InMemoryClientRegistrationRepository(registration);
        }

        @Bean
        OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository
        ) {
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }

        @Bean
        OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
        ) {
            AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                    clientRegistrationRepository,
                    authorizedClientService
                );

            manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().build()
            );
            return manager;
        }
    }

    @RestController
    static class DummyProductController {

        @GetMapping("/api/products/{productId:\\d+}")
        String getProduct(@PathVariable Long productId) {
            return "product-detail";
        }

        @GetMapping("/api/products/{productId:\\d+}/prices")
        String getPriceHistory(@PathVariable Long productId) {
            return "price-history";
        }
    }

    @RestController
    static class DummyEmailVerificationController {

        @GetMapping("/api/auth/email/verify")
        String verifyEmail() {
            return "email-verified";
        }
    }

    @RestController
    @RequestMapping("/api/posts")
    static class DummyPostController {

        @GetMapping("/{postId}")
        String getPost(@PathVariable Long postId) {
            return "post-detail";
        }

        @PostMapping
        @PreAuthorize("hasRole('USER')")
        String createPost(@RequestBody String ignored) {
            return "created";
        }

        @DeleteMapping("/{postId}")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        String deletePost(@PathVariable Long postId) {
            return "deleted";
        }

        @PostMapping("/{postId}/like")
        @PreAuthorize("hasRole('USER')")
        String likePost(@PathVariable Long postId) {
            return "liked";
        }

        @PutMapping("/{postId}/purchase-vote")
        @PreAuthorize("hasRole('USER')")
        String votePurchase(@PathVariable Long postId) {
            return "purchase-voted";
        }
    }

    @RestController
    @RequestMapping("/api/posts/{postId}/comments")
    static class DummyCommentController {

        @GetMapping
        String getComments(@PathVariable Long postId) {
            return "comments";
        }

        @PutMapping("/{commentId}")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        String updateComment(@PathVariable Long postId, @PathVariable Long commentId,
                             @RequestBody String ignored) {
            return "comment-updated";
        }
    }

    @RestController
    @RequestMapping("/api/user/account")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    static class DummyAccountController {

        @GetMapping
        String getAccount() {
            return "account";
        }
    }

    @RestController
    @RequestMapping({"/api/files", "/api/files/s3"})
    static class DummyFileController {

        @GetMapping("/presigned-url")
        String getPresignedUrl() {
            return "file";
        }
    }

    @RestController
    @RequestMapping("/api/ai")
    static class DummyAiController {

        @GetMapping("/ping")
        String ping() {
            return "ai";
        }
    }

    @RestController
    @RequestMapping("/api/ingest")
    static class DummyIngestController {

        @GetMapping("/ping")
        String ping() {
            return "ingest";
        }
    }

    @RestController
    static class DummyPriceCheckController {

        @PostMapping("/api/price-checks")
        String checkPrice(@RequestBody String ignored) {
            return "price-check";
        }
    }

    @RestController
    @RequestMapping("/api/admin/collection-jobs")
    static class DummyAdminProductCollectionController {

        @PostMapping("/manual")
        String collect() {
            return "manual-collected";
        }
    }

    @RestController
    @RequestMapping("/api/admin/launch-news")
    static class DummyAdminLaunchNewsController {

        @PostMapping("/manual")
        String postLaunchNews() {
            return "launch-news-posted";
        }
    }

}
