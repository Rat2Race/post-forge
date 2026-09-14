package dev.iamrat.board.post.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostDetailResponseTest {

    @Test
    @DisplayName("게시글 상세 응답은 첨부 파일을 매핑한다")
    void from_mapsAttachedFiles() {
        Post post = Post.create("title", "content", "summary", List.of("tag"), null, 1L, "writer");
        post.getFiles().add(PostFile.builder()
            .id(10L)
            .originalFileName("photo.png")
            .fileType("image/png")
            .build());

        PostDetailResponse response = PostDetailResponse.from(post, false, 3L, 2, 7L);

        assertThat(response.files()).hasSize(1);
        assertThat(response.files().get(0).fileId()).isEqualTo(10L);
        assertThat(response.files().get(0).originalFileName()).isEqualTo("photo.png");
        assertThat(response.files().get(0).fileType()).isEqualTo("image/png");
        assertThat(response.references()).isEmpty();
        assertThat(response.publishOrigin()).isEqualTo(PostPublishOrigin.USER);
    }

    @Test
    @DisplayName("게시글 상세 응답은 분야 카테고리를 노출한다")
    void from_exposesBoardCategory() {
        Post post = Post.create(
            "title",
            "content",
            "summary",
            List.of("tag"),
            null,
            BoardCategory.BEAUTY,
            PostPublishOrigin.USER,
            1L,
            "writer"
        );

        PostDetailResponse response = PostDetailResponse.from(post, false, 0L, 0, 0L);

        assertThat(response.boardCategory()).isEqualTo(BoardCategory.BEAUTY);
    }
}
