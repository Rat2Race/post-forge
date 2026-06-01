package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.comment.presentation.dto.CommentSummaryResponse;
import dev.iamrat.board.post.application.PostReader;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

    @Mock
    private CommentStore commentStore;

    @Mock
    private CommentReader commentReader;

    @Mock
    private PostReader postReader;

    @Mock
    private AccountProfileReader accountProfileReader;

    private CommentCommandService commentCommandService;

    @BeforeEach
    void setUp() {
        commentCommandService = new CommentCommandService(
            commentStore,
            commentReader,
            postReader,
            accountProfileReader
        );
    }

    @Test
    @DisplayName("댓글 생성 시 account profile 포트의 닉네임을 저장한다")
    void saveComment_usesAccountProfileNickname() {
        Post post = Post.builder()
            .id(1L)
            .title("title")
            .content("content")
            .accountId(10L)
            .nickname("writer")
            .build();
        given(postReader.getById(1L)).willReturn(post);
        given(accountProfileReader.getProfile(2L)).willReturn(new AccountProfile(2L, "댓글러"));

        CommentSummaryResponse response = commentCommandService.saveComment(
            1L,
            null,
            "댓글 본문",
            2L
        );

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentStore).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().getNickname()).isEqualTo("댓글러");
        assertThat(response.nickname()).isEqualTo("댓글러");
    }
}
