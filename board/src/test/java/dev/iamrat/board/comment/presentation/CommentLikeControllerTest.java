package dev.iamrat.board.comment.presentation;

import dev.iamrat.board.comment.application.CommentCommandService;
import dev.iamrat.board.comment.application.CommentInteractionService;
import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.account.UserPrincipal;
import dev.iamrat.board.support.web.TestExceptionResponseHandler;
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
class CommentLikeControllerTest {

    @Mock
    private CommentCommandService commentCommandService;

    @Mock
    private CommentQueryService commentQueryService;

    @Mock
    private CommentInteractionService commentInteractionService;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new TestExceptionResponseHandler())
                .setCustomArgumentResolvers(new TestUserPrincipalResolver())
                .build();
    }

    @Test
    @DisplayName("POST /comments/{id}/like 는 좋아요 응답을 반환한다")
    void likeComment_returnsLikeResponse() throws Exception {
        given(commentInteractionService.likeComment(2L, 1L)).willReturn(new LikeResult(true, 4L));

        mockMvc.perform(post("/posts/1/comments/2/like").with(user(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(true))
                .andExpect(jsonPath("$.likeCount").value(4L));
    }

    @Test
    @DisplayName("댓글 좋아요 요청이 rate limit 에 걸리면 429를 반환한다")
    void likeComment_whenTooManyRequests_returns429() throws Exception {
        given(commentInteractionService.likeComment(2L, 1L))
            .willThrow(new CustomException(CommonErrorCode.TOO_MANY_REQUESTS));

        mockMvc.perform(post("/posts/1/comments/2/like").with(user(1L)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
    }

    private RequestPostProcessor user(Long accountId) {
        return request -> {
            SecurityContext context = new SecurityContextImpl(authentication(accountId));
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);
            request.setSession(session);
            return request;
        };
    }

    private UsernamePasswordAuthenticationToken authentication(Long accountId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                principal(accountId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UserPrincipal principal(Long accountId) {
        return new UserPrincipal() {
            @Override
            public Long getAccountId() {
                return accountId;
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
