package dev.iamrat.security.config;

import dev.iamrat.oauth.handler.OAuth2FailureHandler;
import dev.iamrat.oauth.handler.OAuth2SuccessHandler;
import dev.iamrat.oauth.service.CustomOAuth2UserService;
import dev.iamrat.token.handler.JwtAccessDeniedHandler;
import dev.iamrat.token.handler.JwtAuthenticationEntryPoint;
import dev.iamrat.token.provider.JwtProvider;
import dev.iamrat.login.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = "internal.api-key=test-internal-key")
class SecurityConfigRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
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
    @DisplayName("프로필 조회는 익명 사용자를 차단한다")
    void getProfile_rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/user/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("프로필 조회는 USER 권한이면 허용한다")
    void getProfile_allowsUserRole() throws Exception {
        mockMvc.perform(get("/user/profile"))
            .andExpect(status().isOk())
            .andExpect(content().string("profile"));
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

    @EnableAutoConfiguration
    @Import({
        SecurityConfig.class,
        PasswordEncoderConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        DummyPostController.class,
        DummyCommentController.class,
        DummyProfileController.class
    })
    static class TestApp {

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
    @RequestMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    static class DummyProfileController {

        @GetMapping
        String getProfile() {
            return "profile";
        }
    }
}
