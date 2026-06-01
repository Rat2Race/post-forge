package dev.iamrat.app.config.security;

import dev.iamrat.auth.security.infrastructure.handler.JwtAccessDeniedHandler;
import dev.iamrat.auth.security.infrastructure.handler.JwtAuthenticationEntryPoint;
import dev.iamrat.auth.token.application.TokenService;
import dev.iamrat.auth.login.application.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;

@SpringBootTest(classes = SecurityConfigRegressionTest.TestApp.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
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
    private CustomUserDetailsService customUserDetailsService;

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
    @WithMockUser(roles = "USER")
    @DisplayName("프로필 닉네임 변경은 USER 권한이면 허용한다")
    void updateProfileNickname_allowsUserRole() throws Exception {
        mockMvc.perform(patch("/user/profile/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().string("profile-nickname"));
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
    @WithMockUser(roles = "USER")
    @DisplayName("명시되지 않은 route는 인증된 사용자도 차단한다")
    void undeclaredRoute_deniesAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/undeclared-route"))
            .andExpect(status().isForbidden());
    }

    @EnableAutoConfiguration
    @Import({
        SecurityConfig.class,
        PostForgeAuthorizationRules.class,
        PasswordEncoderConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        DummyPostController.class,
        DummyCommentController.class,
        DummyAccountController.class,
        DummyProfileController.class
    })
    static class TestApp {
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
    @RequestMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    static class DummyProfileController {

        @GetMapping
        String getProfile() {
            return "profile";
        }

        @PatchMapping("/nickname")
        String updateNickname(@RequestBody String ignored) {
            return "profile-nickname";
        }
    }

}
