package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostWriteCommand;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardPostWriterTest {

    @Mock
    private PostStore postStore;

    @InjectMocks
    private BoardPostWriter boardPostWriter;

    @Test
    @DisplayName("PostWriteCommand를 board Post로 저장하고 저장된 ID를 반환한다")
    void write_savesPostFromCommand_returnsSavedId() {
        given(postStore.save(any(Post.class))).willReturn(Post.builder()
            .id(99L)
            .title("AI title")
            .content("AI content")
            .summary("AI summary")
            .tags(List.of("ai", "news"))
            .accountId(null)
            .nickname("AI 분석가")
            .category(PostCategory.DAILY_DIGEST)
            .build());

        Long savedId = boardPostWriter.write(new PostWriteCommand(
            "AI title",
            "AI content",
            "AI summary",
            List.of("ai", "news"),
            null,
            "AI 분석가",
            PostCategory.DAILY_DIGEST,
            null,
            null
        ));

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());

        Post post = postCaptor.getValue();
        assertThat(savedId).isEqualTo(99L);
        assertThat(post.getTitle()).isEqualTo("AI title");
        assertThat(post.getContent()).isEqualTo("AI content");
        assertThat(post.getSummary()).isEqualTo("AI summary");
        assertThat(post.getTags()).containsExactly("ai", "news");
        assertThat(post.getAccountId()).isNull();
        assertThat(post.getNickname()).isEqualTo("AI 분석가");
        assertThat(post.getCategory()).isEqualTo(PostCategory.DAILY_DIGEST);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.USER);
    }

    @Test
    @DisplayName("PostWriteCommand의 발행 출처를 보존한다")
    void write_preservesPublishOrigin() {
        given(postStore.save(any(Post.class))).willReturn(Post.builder()
            .id(100L)
            .title("launch")
            .content("content")
            .nickname("system")
            .category(PostCategory.PRODUCT_LAUNCH_NEWS)
            .publishOrigin(PostPublishOrigin.SYSTEM_BATCH)
            .build());

        boardPostWriter.write(new PostWriteCommand(
            "launch",
            "content",
            "summary",
            List.of("news"),
            null,
            "system",
            PostCategory.PRODUCT_LAUNCH_NEWS,
            null,
            PostPublishOrigin.SYSTEM_BATCH
        ));

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());

        assertThat(postCaptor.getValue().getCategory()).isEqualTo(PostCategory.PRODUCT_LAUNCH_NEWS);
        assertThat(postCaptor.getValue().getPublishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("PostWriteCommand의 분야 카테고리를 저장되는 Post에 반영한다")
    void write_appliesBoardCategoryFromCommand() {
        given(postStore.save(any(Post.class))).willReturn(Post.builder()
            .id(101L)
            .title("launch")
            .content("content")
            .nickname("system")
            .build());

        boardPostWriter.write(new PostWriteCommand(
            "launch",
            "content",
            "summary",
            List.of("news"),
            null,
            "system",
            PostCategory.PRODUCT_LAUNCH_NEWS,
            BoardCategory.DIGITAL,
            PostPublishOrigin.SYSTEM_BATCH
        ));

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());

        assertThat(postCaptor.getValue().getBoardCategory()).isEqualTo(BoardCategory.DIGITAL);
    }
}
