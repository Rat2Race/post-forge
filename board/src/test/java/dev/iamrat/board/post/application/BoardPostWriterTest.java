package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
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
            .category(PostCategory.AI_ANALYSIS)
            .build());

        Long savedId = boardPostWriter.write(new PostWriteCommand(
            "AI title",
            "AI content",
            "AI summary",
            List.of("ai", "news"),
            null,
            "AI 분석가",
            PostCategory.AI_ANALYSIS
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
        assertThat(post.getCategory()).isEqualTo(PostCategory.AI_ANALYSIS);
    }
}
