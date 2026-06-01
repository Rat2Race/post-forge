package dev.iamrat.board.integration;

import dev.iamrat.board.comment.application.CommentCommandService;
import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.comment.presentation.dto.CommentDetailResponse;
import dev.iamrat.board.comment.presentation.dto.CommentSummaryResponse;
import dev.iamrat.board.integration.security.WithMockAccount;
import dev.iamrat.board.post.application.PostCommandService;
import dev.iamrat.board.post.presentation.dto.PostSummaryResponse;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileManager;
import dev.iamrat.core.account.AccountProfileReader;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class CommentIntegrationTest {

    @Autowired
    private PostCommandService postCommandService;

    @Autowired
    private CommentCommandService commentCommandService;

    @Autowired
    private CommentQueryService commentQueryService;

    @MockitoBean
    private AccountProfileReader accountProfileReader;

    @MockitoBean
    private AccountProfileManager accountProfileManager;

    @Test
    @Transactional
    @WithMockAccount
    @DisplayName("일반 댓글은 생성 후 조회 시 그대로 유지된다")
    void createAndReadComment() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "테스터"));
        given(accountProfileReader.getProfile(2L)).willReturn(new AccountProfile(2L, "댓글러"));

        PostSummaryResponse savedPost = postCommandService.savePost(
            "댓글 통합 테스트",
            "댓글 통합 테스트용 게시글 본문입니다.",
            1L
        );

        CommentSummaryResponse savedComment = commentCommandService.saveComment(
            savedPost.id(),
            null,
            "자동 답변 없이 남아야 하는 일반 댓글입니다.",
            2L
        );

        Page<CommentDetailResponse> comments = commentQueryService.getCommentsByPost(
            savedPost.id(),
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")),
            2L
        );

        assertThat(savedComment.parentId()).isNull();
        assertThat(savedComment.nickname()).isEqualTo("댓글러");
        assertThat(comments.getContent()).hasSize(1);
        assertThat(comments.getContent().getFirst().content()).isEqualTo("자동 답변 없이 남아야 하는 일반 댓글입니다.");
        assertThat(comments.getContent().getFirst().replyCount()).isZero();
    }
}
