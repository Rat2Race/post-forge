package dev.iamrat.board.integration;

import dev.iamrat.board.comment.dto.CommentDetailResponse;
import dev.iamrat.board.comment.dto.CommentSummaryResponse;
import dev.iamrat.board.comment.service.CommentService;
import dev.iamrat.board.integration.security.WithMockMember;
import dev.iamrat.board.post.dto.PostSummaryResponse;
import dev.iamrat.board.post.service.PostService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class CommentIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Test
    @Transactional
    @WithMockMember
    @DisplayName("일반 댓글은 생성 후 조회 시 그대로 유지된다")
    void createAndReadComment() {
        PostSummaryResponse savedPost = postService.savePost(
            "댓글 통합 테스트",
            "댓글 통합 테스트용 게시글 본문입니다.",
            "testuser",
            "테스터",
            List.of()
        );

        CommentSummaryResponse savedComment = commentService.saveComment(
            savedPost.id(),
            null,
            "자동 답변 없이 남아야 하는 일반 댓글입니다.",
            "commenter",
            "댓글러"
        );

        Page<CommentDetailResponse> comments = commentService.getCommentsByPost(
            savedPost.id(),
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")),
            "commenter"
        );

        assertThat(savedComment.parentId()).isNull();
        assertThat(comments.getContent()).hasSize(1);
        assertThat(comments.getContent().getFirst().content()).isEqualTo("자동 답변 없이 남아야 하는 일반 댓글입니다.");
        assertThat(comments.getContent().getFirst().replyCount()).isZero();
    }
}
