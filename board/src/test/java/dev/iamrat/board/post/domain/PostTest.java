package dev.iamrat.board.post.domain;

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
        assertThat(post.getAccountId()).isEqualTo(1L);
        assertThat(post.getNickname()).isEqualTo("writer");
        assertThat(post.getTags()).isEmpty();
    }

    @Test
    @DisplayName("게시글 생성 시 태그를 복사한다")
    void create_copiesTags() {
        List<String> tags = List.of("db", "isolation");

        Post post = Post.create("title", "content", tags, 1L, "writer");

        assertThat(post.getTags()).containsExactly("db", "isolation");
        assertThat(post.getTags()).isNotSameAs(tags);
    }

    @Test
    @DisplayName("게시글 수정은 제목·내용·태그를 바꾼다")
    void update_changesTitleContentTags() {
        Post post = Post.create("old title", "old content", List.of("old"), 1L, "writer");

        post.update("new title", "new content", List.of("new"));

        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
        assertThat(post.getTags()).containsExactly("new");
    }

    @Test
    @DisplayName("게시글 수정 시 태그가 null이면 빈 목록으로 대체한다")
    void update_nullTagsBecomesEmptyList() {
        Post post = Post.general("title", "content", 1L, "writer");

        post.update("title", "content", null);

        assertThat(post.getTags()).isEmpty();
    }
}
