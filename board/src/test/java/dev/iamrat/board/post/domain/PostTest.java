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
        List<String> tags = List.of("ai", "news");

        Post post = Post.create(
            "AI title",
            "AI content",
            "AI summary",
            tags,
            PostCategory.AI_ANALYSIS,
            null,
            "AI 분석가"
        );

        assertThat(post.getTitle()).isEqualTo("AI title");
        assertThat(post.getSummary()).isEqualTo("AI summary");
        assertThat(post.getTags()).containsExactly("ai", "news");
        assertThat(post.getTags()).isNotSameAs(tags);
        assertThat(post.getCategory()).isEqualTo(PostCategory.AI_ANALYSIS);
        assertThat(post.getAccountId()).isNull();
        assertThat(post.getNickname()).isEqualTo("AI 분석가");
    }

    @Test
    void create_nullCategoryDefaultsToGeneral() {
        Post post = Post.create("title", "content", null, null, null, 1L, "writer");

        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(post.getTags()).isEmpty();
    }
}
