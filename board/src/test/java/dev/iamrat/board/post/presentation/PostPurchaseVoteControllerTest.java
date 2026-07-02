package dev.iamrat.board.post.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.iamrat.board.post.application.PostCommandService;
import dev.iamrat.board.post.application.PostInteractionService;
import dev.iamrat.board.post.application.PostQueryService;
import dev.iamrat.board.purchase.application.PurchaseVoteService;
import dev.iamrat.board.purchase.application.PurchaseVoteSummary;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import dev.iamrat.core.account.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
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

@ExtendWith(MockitoExtension.class)
class PostPurchaseVoteControllerTest {

    @Mock
    private PostCommandService postCommandService;

    @Mock
    private PostQueryService postQueryService;

    @Mock
    private PostInteractionService postInteractionService;

    @Mock
    private PurchaseVoteService purchaseVoteService;

    @InjectMocks
    private PostController postController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
            .setCustomArgumentResolvers(new TestUserPrincipalResolver())
            .build();
    }

    @Test
    @DisplayName("구매 투표 등록은 갱신된 요약을 반환한다")
    void putPurchaseVoteReturnsSummary() throws Exception {
        given(purchaseVoteService.vote(1L, 9L, PurchaseVoteType.BUYABLE)).willReturn(new PurchaseVoteSummary(
            1L,
            true,
            3L,
            1L,
            0L,
            PurchaseVoteType.BUYABLE
        ));

        mockMvc.perform(put("/api/posts/1/purchase-vote")
                .with(user(9L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"voteType\":\"BUYABLE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.postId").value(1L))
            .andExpect(jsonPath("$.eligible").value(true))
            .andExpect(jsonPath("$.buyableCount").value(3L))
            .andExpect(jsonPath("$.myVote").value("BUYABLE"));
    }

    @Test
    @DisplayName("구매 투표 삭제는 갱신된 요약을 반환한다")
    void deletePurchaseVoteReturnsUpdatedSummary() throws Exception {
        given(purchaseVoteService.unvote(1L, 9L)).willReturn(PurchaseVoteSummary.empty(1L, true));

        mockMvc.perform(delete("/api/posts/1/purchase-vote").with(user(9L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.postId").value(1L))
            .andExpect(jsonPath("$.eligible").value(true))
            .andExpect(jsonPath("$.myVote").doesNotExist());
    }

    @Test
    @DisplayName("잘못된 투표 enum 값이면 검증 오류를 반환한다")
    void invalidVoteEnumReturnsValidationError() throws Exception {
        mockMvc.perform(put("/api/posts/1/purchase-vote")
                .with(user(9L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"voteType\":\"NOPE\"}"))
            .andExpect(status().isBadRequest());
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
        return () -> accountId;
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
            var request = webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class);
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
