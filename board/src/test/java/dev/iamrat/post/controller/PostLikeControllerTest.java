package dev.iamrat.post.controller;

import dev.iamrat.global.exception.CommonErrorCode;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.support.web.GlobalExceptionHandler;
import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.post.service.PostService;
import dev.iamrat.global.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostLikeControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestUserPrincipalResolver())
                .build();
    }

    @Test
    @DisplayName("POST /posts/{id}/like 는 좋아요 응답을 반환한다")
    void likePost_returnsLikeResponse() throws Exception {
        given(postService.likePost(1L, "user1")).willReturn(new LikeResponse(true, 3L));

        mockMvc.perform(post("/posts/1/like").with(user("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(true))
                .andExpect(jsonPath("$.likeCount").value(3L));
    }

    @Test
    @DisplayName("좋아요 요청이 rate limit 에 걸리면 429를 반환한다")
    void likePost_whenTooManyRequests_returns429() throws Exception {
        given(postService.likePost(1L, "user1")).willThrow(new CustomException(CommonErrorCode.TOO_MANY_REQUESTS));

        mockMvc.perform(post("/posts/1/like").with(user("user1")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
    }

    private RequestPostProcessor user(String userId) {
        return request -> {
            SecurityContext context = new SecurityContextImpl(authentication(userId));
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);
            request.setSession(session);
            return request;
        };
    }

    private UsernamePasswordAuthenticationToken authentication(String userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                principal(userId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UserPrincipal principal(String userId) {
        return new UserPrincipal() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public String getNickname() {
                return "tester";
            }
        };
    }

    private static class TestUserPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal.class)
                    && UserPrincipal.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request == null || request.getSession(false) == null) {
                return null;
            }
            Object context = request.getSession(false).getAttribute("SPRING_SECURITY_CONTEXT");
            if (context instanceof SecurityContext securityContext && securityContext.getAuthentication() != null) {
                return securityContext.getAuthentication().getPrincipal();
            }
            return null;
        }
    }
}
