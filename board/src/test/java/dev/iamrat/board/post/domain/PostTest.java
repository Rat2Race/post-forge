package dev.iamrat.board.post.domain;

import dev.iamrat.core.board.post.PostCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    void general_createsGeneralPostWithWriterProfile() {
        Post post = Post.general("title", "content", 1L, "writer");

        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getContent()).isEqualTo("content");
        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(post.getAccountId()).isEqualTo(1L);
        assertThat(post.getNickname()).isEqualTo("writer");
        assertThat(post.getTags()).isEmpty();
    }

    @Test
    void create_copiesTagsAndUsesExplicitCategory() {
        List<String> tags = List.of("product", "trend");

        Post post = Post.create(
            "title",
            "content",
            "summary",
            tags,
            PostCategory.GENERAL,
            1L,
            "writer"
        );

        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getSummary()).isEqualTo("summary");
        assertThat(post.getTags()).containsExactly("product", "trend");
        assertThat(post.getTags()).isNotSameAs(tags);
        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(post.getAccountId()).isEqualTo(1L);
        assertThat(post.getNickname()).isEqualTo("writer");
    }

    @Test
    void update_changesTitleAndContent() {
        Post post = Post.general("old title", "old content", 1L, "writer");

        post.update("new title", "new content");

        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
    }
}
