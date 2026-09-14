package dev.iamrat.board.post.domain;

import dev.iamrat.core.board.post.BoardCategory;
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
            PostCategory.DAILY_DIGEST,
            null,
            "AI 분석가"
        );

        assertThat(post.getTitle()).isEqualTo("AI title");
        assertThat(post.getSummary()).isEqualTo("AI summary");
        assertThat(post.getTags()).containsExactly("ai", "news");
        assertThat(post.getTags()).isNotSameAs(tags);
        assertThat(post.getCategory()).isEqualTo(PostCategory.DAILY_DIGEST);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.USER);
        assertThat(post.getAccountId()).isNull();
        assertThat(post.getNickname()).isEqualTo("AI 분석가");
    }

    @Test
    @DisplayName("카테고리가 null이면 일반 카테고리를 기본값으로 사용한다")
    void create_nullCategoryDefaultsToGeneral() {
        Post post = Post.create("title", "content", null, null, null, 1L, "writer");

        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
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
            PostPublishOrigin.SYSTEM_BATCH,
            null,
            "system"
        );

        assertThat(post.getCategory()).isEqualTo(PostCategory.PRODUCT_LAUNCH_NEWS);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("게시글 수정은 제목·내용·태그만 바꾸고 요약과 카테고리는 유지한다")
    void update_changesTitleContentTagsOnly() {
        Post post = Post.create(
            "old title",
            "old content",
            "summary",
            List.of("old"),
            PostCategory.GENERAL,
            1L,
            "writer"
        );

        post.update("new title", "new content", List.of("new"));

        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
        assertThat(post.getTags()).containsExactly("new");
        assertThat(post.getSummary()).isEqualTo("summary");
        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
    }

    @Test
    @DisplayName("출시 뉴스 게시글을 수정해도 카테고리가 유지된다")
    void update_preservesLaunchNewsCategory() {
        Post post = Post.create(
            "launch title",
            "launch content",
            "launch summary",
            List.of("launch"),
            PostCategory.PRODUCT_LAUNCH_NEWS,
            1L,
            "writer"
        );

        post.update("edited title", "edited content", List.of("edited"));

        assertThat(post.getCategory()).isEqualTo(PostCategory.PRODUCT_LAUNCH_NEWS);
    }

    @Test
    @DisplayName("게시글 생성 시 명시한 분야 카테고리를 사용한다")
    void create_usesExplicitBoardCategory() {
        Post post = Post.create(
            "title",
            "content",
            "summary",
            List.of("tag"),
            PostCategory.GENERAL,
            BoardCategory.DIGITAL,
            PostPublishOrigin.USER,
            1L,
            "writer"
        );

        assertThat(post.getBoardCategory()).isEqualTo(BoardCategory.DIGITAL);
    }

    @Test
    @DisplayName("분야 카테고리가 null이면 GENERAL을 기본값으로 사용한다")
    void create_nullBoardCategoryDefaultsToGeneral() {
        Post post = Post.create(
            "title",
            "content",
            "summary",
            List.of("tag"),
            PostCategory.GENERAL,
            null,
            PostPublishOrigin.USER,
            1L,
            "writer"
        );

        assertThat(post.getBoardCategory()).isEqualTo(BoardCategory.GENERAL);
    }

    @Test
    @DisplayName("게시글 수정은 분야 카테고리를 유지한다")
    void update_preservesBoardCategory() {
        Post post = Post.create(
            "title",
            "content",
            "summary",
            List.of("tag"),
            PostCategory.GENERAL,
            BoardCategory.APPLIANCE,
            PostPublishOrigin.USER,
            1L,
            "writer"
        );

        post.update("new title", "new content", List.of("new"));

        assertThat(post.getBoardCategory()).isEqualTo(BoardCategory.APPLIANCE);
    }

    @Test
    @DisplayName("게시글 수정 시 태그가 null이면 빈 목록으로 대체한다")
    void update_nullTagsBecomesEmptyList() {
        Post post = Post.general("title", "content", 1L, "writer");

        post.update("title", "content", null);

        assertThat(post.getTags()).isEmpty();
    }
}
