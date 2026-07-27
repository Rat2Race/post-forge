package dev.iamrat.board.post.domain;

import dev.iamrat.core.board.post.PostBoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    @DisplayName("일반 게시글은 작성자 프로필을 포함해 생성된다")
    void general_createsGeneralPostWithWriterProfile() {
        Post post = Post.general("title", "content", 1L, "writer");

        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getContent()).isEqualTo("content");
        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(post.getBoardCategory()).isEqualTo(PostBoardCategory.GENERAL);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.USER);
        assertThat(post.getAccountId()).isEqualTo(1L);
        assertThat(post.getNickname()).isEqualTo("writer");
        assertThat(post.getTags()).isEmpty();
    }

    @Test
    @DisplayName("게시글 생성 시 태그를 복사하고 명시한 카테고리를 사용한다")
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
        assertThat(post.getBoardCategory()).isEqualTo(PostBoardCategory.GENERAL);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.USER);
        assertThat(post.getAccountId()).isNull();
        assertThat(post.getNickname()).isEqualTo("AI 분석가");
    }

    @Test
    @DisplayName("카테고리가 null이면 일반 카테고리를 기본값으로 사용한다")
    void create_nullCategoryDefaultsToGeneral() {
        Post post = Post.create("title", "content", null, null, null, 1L, "writer");

        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(post.getBoardCategory()).isEqualTo(PostBoardCategory.GENERAL);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.USER);
        assertThat(post.getTags()).isEmpty();
    }

    @Test
    @DisplayName("출시 뉴스 게시글은 명시한 발행 출처를 사용한다")
    void create_usesExplicitPublishOriginForLaunchNews() {
        Post post = Post.create(
            "launch title",
            "launch content",
            "launch summary",
            List.of("launch"),
            PostCategory.PRODUCT_LAUNCH_NEWS,
            PostBoardCategory.DIGITAL,
            PostPublishOrigin.SYSTEM_BATCH,
            null,
            "system"
        );

        assertThat(post.getCategory()).isEqualTo(PostCategory.PRODUCT_LAUNCH_NEWS);
        assertThat(post.getBoardCategory()).isEqualTo(PostBoardCategory.DIGITAL);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }
}
