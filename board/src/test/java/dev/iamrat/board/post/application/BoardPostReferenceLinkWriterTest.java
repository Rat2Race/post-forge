package dev.iamrat.board.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceLinkCommand;
import dev.iamrat.core.board.post.PostReferenceProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardPostReferenceLinkWriterTest {

    @Mock
    private PostStore postStore;

    @Mock
    private PostReferenceLinkStore postReferenceLinkStore;

    @InjectMocks
    private BoardPostReferenceLinkWriter writer;

    @Test
    @DisplayName("참조 근거 링크를 board store를 통해 저장한다")
    void write_savesReferenceEvidenceThroughBoardStore() {
        Post post = Post.builder().id(10L).title("title").content("content").nickname("system").build();
        given(postStore.getReferenceById(10L)).willReturn(post);
        given(postReferenceLinkStore.save(any(PostReferenceLink.class))).willReturn(PostReferenceLink.builder()
            .id(99L)
            .post(post)
            .provider(PostReferenceProvider.NAVER_NEWS)
            .canonicalUrl("https://news.example/a")
            .originalUrl("https://news.example/a?utm=1")
            .sourceName("Example")
            .titleSnapshot("Launch")
            .publishOrigin(PostPublishOrigin.SYSTEM_BATCH)
            .build());

        Long id = writer.write(new PostReferenceLinkCommand(
            10L,
            "갤럭시북",
            20L,
            PostReferenceProvider.NAVER_NEWS,
            "https://news.example/a",
            "https://news.example/a?utm=1",
            "Example",
            LocalDateTime.of(2026, 6, 21, 10, 0),
            "Launch",
            PostPublishOrigin.SYSTEM_BATCH
        ));

        ArgumentCaptor<PostReferenceLink> captor = ArgumentCaptor.forClass(PostReferenceLink.class);
        verify(postReferenceLinkStore).save(captor.capture());

        assertThat(id).isEqualTo(99L);
        assertThat(captor.getValue().getPost()).isSameAs(post);
        assertThat(captor.getValue().getKeyword()).isEqualTo("갤럭시북");
        assertThat(captor.getValue().getProductId()).isEqualTo(20L);
        assertThat(captor.getValue().getCanonicalUrl()).isEqualTo("https://news.example/a");
        assertThat(captor.getValue().getPublishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }
}
