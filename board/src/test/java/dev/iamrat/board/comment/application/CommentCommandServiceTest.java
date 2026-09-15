package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.comment.presentation.CommentSummaryResponse;
import dev.iamrat.board.post.application.PostReader;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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

    @Test
    @DisplayName("답글 생성 시 부모 댓글을 검증하고 부모에 연결한다")
    void saveComment_withParent_linksReplyToParent() {
        Post post = Post.builder()
            .id(1L)
            .title("title")
            .content("content")
            .accountId(10L)
            .nickname("writer")
            .build();
        Comment parent = Comment.builder()
            .id(5L)
            .post(post)
            .content("부모 댓글")
            .accountId(3L)
            .nickname("부모작성자")
            .build();
        given(postReader.getById(1L)).willReturn(post);
        given(commentReader.getById(5L)).willReturn(parent);
        given(accountProfileReader.getProfile(2L)).willReturn(new AccountProfile(2L, "댓글러"));

        commentCommandService.saveComment(1L, 5L, "답글 본문", 2L);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentStore).save(commentCaptor.capture());
        Comment saved = commentCaptor.getValue();
        assertThat(saved.getParent()).isEqualTo(parent);
        assertThat(parent.getReplies()).contains(saved);
    }

    @Test
    @DisplayName("부모 댓글이 다른 게시글 소속이면 INVALID_COMMENT_PARENT 예외를 전파한다")
    void saveComment_withParentOfOtherPost_propagatesValidationException() {
        Post post = Post.builder()
            .id(1L)
            .title("title")
            .content("content")
            .accountId(10L)
            .nickname("writer")
            .build();
        Post otherPost = Post.builder()
            .id(2L)
            .title("other")
            .content("content")
            .accountId(10L)
            .nickname("writer")
            .build();
        Comment parent = Comment.builder()
            .id(5L)
            .post(otherPost)
            .content("부모 댓글")
            .accountId(3L)
            .nickname("부모작성자")
            .build();
        given(postReader.getById(1L)).willReturn(post);
        given(commentReader.getById(5L)).willReturn(parent);
        given(accountProfileReader.getProfile(2L)).willReturn(new AccountProfile(2L, "댓글러"));

        assertThatThrownBy(() -> commentCommandService.saveComment(1L, 5L, "답글 본문", 2L))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(BoardErrorCode.INVALID_COMMENT_PARENT);
        verify(commentStore, never()).save(any(Comment.class));
    }
}
