package dev.iamrat.auth.security.config;

import dev.iamrat.auth.oauth.handler.OAuth2FailureHandler;
import dev.iamrat.auth.oauth.handler.OAuth2SuccessHandler;
import dev.iamrat.auth.oauth.service.CustomOAuth2UserService;
import dev.iamrat.auth.token.handler.JwtAccessDeniedHandler;
import dev.iamrat.auth.token.handler.JwtAuthenticationEntryPoint;
import dev.iamrat.auth.token.provider.JwtProvider;
import dev.iamrat.auth.login.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.mockito.BDDMockito.willAnswer;

@SpringBootTest(classes = SecurityConfigRegressionTest.TestApp.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "internal.api-key=test-internal-key",
    "monitoring.username=monitor",
    "monitoring.password=monitor-secret"
})
class SecurityConfigRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private OAuth2FailureHandler oAuth2FailureHandler;

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
        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isOk())
            .andExpect(content().string("post-detail"));
    }

    @Test
    @DisplayName("댓글 조회는 인증 없이 허용한다")
    void getComments_allowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/posts/1/comments"))
            .andExpect(status().isOk())
            .andExpect(content().string("comments"));
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
        mockMvc.perform(get("/user/account"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("계정 조회는 USER 권한이면 허용한다")
    void getAccount_allowsUserRole() throws Exception {
        mockMvc.perform(get("/user/account"))
            .andExpect(status().isOk())
            .andExpect(content().string("account"));
    }

    @Test
    @DisplayName("파일 API는 익명 사용자를 차단한다")
    void fileApi_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/files/s3/presigned-url"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("파일 API는 USER 권한이면 허용한다")
    void fileApi_allowsUserRole() throws Exception {
        mockMvc.perform(get("/files/s3/presigned-url"))
            .andExpect(status().isOk())
            .andExpect(content().string("file"));
    }

    @Test
    @DisplayName("게시글 생성은 익명 사용자를 차단한다")
    void createPost_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("게시글 생성은 USER 권한이면 허용한다")
    void createPost_allowsUserRole() throws Exception {
        mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().string("created"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("좋아요는 ADMIN만으로는 허용하지 않는다")
    void likePost_rejectsAdminRole() throws Exception {
        mockMvc.perform(post("/posts/1/like"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("게시글 삭제는 ADMIN 권한이면 허용한다")
    void deletePost_allowsAdminRole() throws Exception {
        mockMvc.perform(delete("/posts/1"))
            .andExpect(status().isOk())
            .andExpect(content().string("deleted"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("댓글 수정은 USER 권한이면 허용한다")
    void updateComment_allowsUserRole() throws Exception {
        mockMvc.perform(put("/posts/1/comments/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().string("comment-updated"));
    }

    @Test
    @DisplayName("AI API는 내부 API 키로 접근을 허용한다")
    void aiApi_allowsInternalApiKey() throws Exception {
        mockMvc.perform(get("/ai/ping")
                .header("X-Internal-Api-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(content().string("ai"));
    }

    @Test
    @DisplayName("Ingest API는 내부 API 키로 접근을 허용한다")
    void ingestApi_allowsInternalApiKey() throws Exception {
        mockMvc.perform(get("/ingest/ping")
                .header("X-Internal-Api-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(content().string("ingest"));
    }

    @Test
    @DisplayName("collector trigger는 익명 사용자를 차단한다")
    void collectorTrigger_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/collector/naver-news"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("collector trigger는 USER 권한을 차단한다")
    void collectorTrigger_rejectsUserRole() throws Exception {
        mockMvc.perform(post("/collector/naver-news"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("collector trigger는 ADMIN 권한이면 허용한다")
    void collectorTrigger_allowsAdminRole() throws Exception {
        mockMvc.perform(post("/collector/naver-news"))
            .andExpect(status().isOk())
            .andExpect(content().string("collected"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("internal collector ingest는 USER 권한을 차단한다")
    void internalCollectorDocuments_rejectsUserRole() throws Exception {
        mockMvc.perform(post("/internal/collector/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("internal collector ingest는 ADMIN 권한이면 허용한다")
    void internalCollectorDocuments_allowsAdminRole() throws Exception {
        mockMvc.perform(post("/internal/collector/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isOk())
            .andExpect(content().string("internal-collected"));
    }

    @Test
    @DisplayName("internal collector ingest는 내부 API 키로 허용한다")
    void internalCollectorDocuments_allowsInternalApiKey() throws Exception {
        mockMvc.perform(post("/internal/collector/documents")
                .header("X-Internal-Api-Key", "test-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isOk())
            .andExpect(content().string("internal-collected"));
    }

    @Test
    @DisplayName("로컬 Test Console route는 인증 필터에서 막지 않는다")
    void testConsoleRoute_allowsAnonymousAccessToReachLocalOnlyController() throws Exception {
        mockMvc.perform(get("/test-console"))
            .andExpect(status().isOk())
            .andExpect(content().string("test-console"));
    }

    @Test
    @DisplayName("로컬 Test Console API route는 인증 필터에서 막지 않는다")
    void testConsoleApi_allowsAnonymousAccessToReachLocalOnlyController() throws Exception {
        mockMvc.perform(get("/api/test-console/state"))
            .andExpect(status().isOk())
            .andExpect(content().string("test-console-state"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("명시되지 않은 route는 인증된 사용자도 차단한다")
    void undeclaredRoute_deniesAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/undeclared-route"))
            .andExpect(status().isForbidden());
    }

    @EnableAutoConfiguration
    @Import({
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
        DummyCollectorController.class,
        DummyInternalCollectorController.class,
        DummyTestConsoleController.class
    })
    static class TestApp {

        @Bean
        HttpAuthorizationRules httpAuthorizationRules() {
            return requests -> requests
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/favicon.ico",
                    "/images/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/webjars/**",
                    "/test-console",
                    "/test-console/**",
                    "/api/test-console",
                    "/api/test-console/**",
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/auth/register",
                    "/auth/login",
                    "/auth/token/reissue",
                    "/auth/oauth2/exchange",
                    "/auth/email/send"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/auth/email/verify",
                    "/posts",
                    "/posts/*",
                    "/posts/*/comments"
                ).permitAll()
                .requestMatchers(
                    "/auth/logout",
                    "/user/account",
                    "/user/account/**",
                    "/posts",
                    "/posts/*",
                    "/posts/*/like",
                    "/posts/*/comments",
                    "/posts/*/comments/*",
                    "/posts/*/comments/*/like",
                    "/files/s3/**",
                    "/ai/**",
                    "/ingest/**"
                ).hasAnyRole("USER", "ADMIN")
                .requestMatchers("/collector/**").hasRole("ADMIN")
                .requestMatchers("/internal/collector/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().denyAll();
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
    @RequestMapping("/posts")
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
    }

    @RestController
    @RequestMapping("/posts/{postId}/comments")
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
    @RequestMapping("/user/account")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    static class DummyAccountController {

        @GetMapping
        String getAccount() {
            return "account";
        }
    }

    @RestController
    @RequestMapping("/files/s3")
    static class DummyFileController {

        @GetMapping("/presigned-url")
        String getPresignedUrl() {
            return "file";
        }
    }

    @RestController
    @RequestMapping("/ai")
    static class DummyAiController {

        @GetMapping("/ping")
        String ping() {
            return "ai";
        }
    }

    @RestController
    @RequestMapping("/ingest")
    static class DummyIngestController {

        @GetMapping("/ping")
        String ping() {
            return "ingest";
        }
    }

    @RestController
    @RequestMapping("/collector")
    static class DummyCollectorController {

        @PostMapping("/naver-news")
        String collect() {
            return "collected";
        }
    }

    @RestController
    @RequestMapping("/internal/collector")
    static class DummyInternalCollectorController {

        @PostMapping("/documents")
        String collectDocuments(@RequestBody String ignored) {
            return "internal-collected";
        }
    }

    @RestController
    static class DummyTestConsoleController {

        @GetMapping("/test-console")
        String page() {
            return "test-console";
        }

        @GetMapping("/api/test-console/state")
        String state() {
            return "test-console-state";
        }
    }
}
