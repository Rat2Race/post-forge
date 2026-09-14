package dev.iamrat.board.comment.domain;

import dev.iamrat.board.post.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    @Test
    @DisplayName("답글 추가 시 부모 댓글에 연결한다")
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
