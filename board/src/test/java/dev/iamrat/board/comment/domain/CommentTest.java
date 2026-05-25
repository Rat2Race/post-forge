package dev.iamrat.board.comment.domain;

import dev.iamrat.board.post.domain.Post;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    @Test
    void create_rootComment_setsPostAndWriterProfile() {
        Post post = post();

        Comment comment = Comment.create(post, null, "댓글 본문", 2L, "댓글러");

        assertThat(comment.getPost()).isSameAs(post);
        assertThat(comment.getParent()).isNull();
        assertThat(comment.getContent()).isEqualTo("댓글 본문");
        assertThat(comment.getAccountId()).isEqualTo(2L);
        assertThat(comment.getNickname()).isEqualTo("댓글러");
        assertThat(comment.getLikeCount()).isZero();
        assertThat(comment.getReplies()).isEmpty();
    }

    @Test
    void create_replyComment_setsParent() {
        Post post = post();
        Comment parent = Comment.create(post, null, "부모 댓글", 2L, "부모");

        Comment reply = Comment.create(post, parent, "대댓글", 3L, "대댓글러");

        assertThat(reply.getPost()).isSameAs(post);
        assertThat(reply.getParent()).isSameAs(parent);
        assertThat(reply.getContent()).isEqualTo("대댓글");
        assertThat(reply.getAccountId()).isEqualTo(3L);
        assertThat(reply.getNickname()).isEqualTo("대댓글러");
    }

    @Test
    void addReply_linksReplyToParent() {
        Post post = post();
        Comment parent = Comment.create(post, null, "부모 댓글", 2L, "부모");
        Comment reply = Comment.create(post, null, "대댓글", 3L, "대댓글러");

        parent.addReply(reply);

        assertThat(parent.getReplies()).containsExactly(reply);
        assertThat(reply.getParent()).isSameAs(parent);
    }

    private Post post() {
        return Post.general("title", "content", 1L, "writer");
    }
}
