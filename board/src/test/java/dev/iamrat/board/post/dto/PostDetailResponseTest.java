package dev.iamrat.board.post.dto;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostDetailResponseTest {

    @Test
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
    }
}
