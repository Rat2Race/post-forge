package dev.iamrat.board.comment.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.iamrat.board.comment.application.CommentCommandService;
import dev.iamrat.board.comment.application.CommentInteractionService;
import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.core.account.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.NoHandlerFoundException;

@ExtendWith(MockitoExtension.class)
class CommentControllerRouteAliasTest {

    @Mock
    private CommentCommandService commentCommandService;

    @Mock
    private CommentQueryService commentQueryService;

    @Mock
    private CommentInteractionService commentInteractionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CommentController commentController = new CommentController(
            commentCommandService,
            commentQueryService,
            commentInteractionService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
            .setCustomArgumentResolvers(
                new TestUserPrincipalResolver(),
                new PageableHandlerMethodArgumentResolver()
            )
            .build();
    }

    @Test
    @DisplayName("GET /api/posts/{postId}/comments 는 댓글 목록을 반환한다")
    void getComments_apiRoute_returnsComments() throws Exception {
        given(commentQueryService.getCommentsByPost(any(Long.class), any(), any()))
            .willReturn(new PageImpl<>(
                List.of(commentDetail()),
                PageRequest.of(0, 50),
                1
            ));

        mockMvc.perform(get("/api/posts/1/comments").with(user(9L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(2L))
            .andExpect(jsonPath("$.content[0].content").value("댓글 본문"));
    }

    @Test
    @DisplayName("POST /api/posts/{postId}/comments 는 댓글을 생성한다")
    void createComment_apiRoute_createsComment() throws Exception {
        given(commentCommandService.saveComment(1L, null, "새 댓글", 9L))
            .willReturn(commentSummary());

        mockMvc.perform(post("/api/posts/1/comments")
                .with(user(9L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"새 댓글\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.content").value("새 댓글"));
    }

    @Test
    @DisplayName("PUT /api/posts/{postId}/comments/{commentId} 는 댓글을 수정한다")
    void updateComment_apiRoute_updatesComment() throws Exception {
        given(commentCommandService.updateComment(2L, "수정 댓글"))
            .willReturn(commentSummary("수정 댓글"));

        mockMvc.perform(put("/api/posts/1/comments/2")
                .with(user(9L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"수정 댓글\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.content").value("수정 댓글"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId}/comments/{commentId} 는 댓글을 삭제한다")
    void deleteComment_apiRoute_deletesComment() throws Exception {
        mockMvc.perform(delete("/api/posts/1/comments/2").with(user(9L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("댓글 삭제 완료"));

        verify(commentCommandService).deleteComment(2L);
    }

    @Test
    @DisplayName("POST /api/posts/{postId}/comments/{commentId}/like 는 댓글 좋아요를 등록한다")
    void likeComment_apiRoute_likesComment() throws Exception {
        given(commentInteractionService.likeComment(2L, 9L)).willReturn(new LikeResult(true, 3L));

        mockMvc.perform(post("/api/posts/1/comments/2/like").with(user(9L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isLiked").value(true))
            .andExpect(jsonPath("$.likeCount").value(3L));
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId}/comments/{commentId}/like 는 댓글 좋아요를 취소한다")
    void unlikeComment_apiRoute_unlikesComment() throws Exception {
        given(commentInteractionService.unlikeComment(2L, 9L)).willReturn(new LikeResult(false, 2L));

        mockMvc.perform(delete("/api/posts/1/comments/2/like").with(user(9L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isLiked").value(false))
            .andExpect(jsonPath("$.likeCount").value(2L));
    }

    @Test
    @DisplayName("기존 /posts/{postId}/comments 경로는 더 이상 매핑하지 않는다")
    void legacyCommentsRoute_isNotMapped() throws Exception {
        mockMvc.perform(get("/posts/1/comments").with(user(9L)))
            .andExpect(result -> assertThat(result.getResolvedException())
                .isInstanceOf(NoHandlerFoundException.class));

        verifyNoInteractions(commentQueryService);
    }

    private CommentDetailResponse commentDetail() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 22, 14, 0);
        return new CommentDetailResponse(
            2L,
            "댓글 본문",
            9L,
            "tester",
            null,
            0,
            3L,
            true,
            now,
            now
        );
    }

    private CommentSummaryResponse commentSummary() {
        return commentSummary("새 댓글");
    }

    private CommentSummaryResponse commentSummary(String content) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 22, 14, 0);
        return new CommentSummaryResponse(2L, content, 9L, "tester", null, now, now);
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
            (UserPrincipal) () -> accountId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private static class TestUserPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                && UserPrincipal.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            org.springframework.web.bind.support.WebDataBinderFactory binderFactory
        ) {
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
