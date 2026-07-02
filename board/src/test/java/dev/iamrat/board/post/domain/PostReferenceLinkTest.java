package dev.iamrat.board.post.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostReferenceLinkTest {

    @Test
    @DisplayName("참조 링크 생성 시 출처 근거를 보존하고 기본 발행 출처를 설정한다")
    void of_capturesSourceEvidenceAndDefaultsPublishOrigin() {
        Post post = Post.builder()
            .id(1L)
            .title("title")
            .content("content")
            .accountId(1L)
            .nickname("writer")
            .build();
        LocalDateTime publishedAt = LocalDateTime.of(2026, 6, 21, 10, 0);

        PostReferenceLink link = PostReferenceLink.of(
            post,
            "GalaxyBook",
            10L,
            PostReferenceProvider.NAVER_NEWS,
            "https://news.example/article",
            "https://news.example/article?utm=1",
            "Example News",
            publishedAt,
            "New product launched",
            null
        );

        assertThat(link.getPost()).isSameAs(post);
        assertThat(link.getKeyword()).isEqualTo("galaxybook");
        assertThat(link.getProductId()).isEqualTo(10L);
        assertThat(link.getProvider()).isEqualTo(PostReferenceProvider.NAVER_NEWS);
        assertThat(link.getCanonicalUrl()).isEqualTo("https://news.example/article");
        assertThat(link.getOriginalUrl()).isEqualTo("https://news.example/article?utm=1");
        assertThat(link.getSourceName()).isEqualTo("Example News");
        assertThat(link.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(link.getTitleSnapshot()).isEqualTo("New product launched");
        assertThat(link.getPublishOrigin()).isEqualTo(PostPublishOrigin.USER);
    }
}
