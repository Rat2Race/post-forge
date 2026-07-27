package dev.iamrat.board.post.application;

import dev.iamrat.board.file.application.FileAttachmentService;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostFileAppenderTest {

    @Mock
    private FileAttachmentService fileAttachmentService;

    @InjectMocks
    private PostFileAppender postFileAppender;

    @Test
    @DisplayName("appendFiles는 파일 첨부를 파일 애플리케이션에 위임한다")
    void appendFiles_delegatesToFileAttachmentService() {
        Post post = post(1L);

        postFileAppender.appendFiles(post, List.of(10L));

        verify(fileAttachmentService).appendFiles(post, List.of(10L));
    }

    @Test
    @DisplayName("replaceFiles는 파일 교체를 파일 애플리케이션에 위임한다")
    void replaceFiles_delegatesToFileAttachmentService() {
        Post post = post(1L);

        postFileAppender.replaceFiles(post, List.of(20L));

        verify(fileAttachmentService).replaceFiles(post, List.of(20L));
    }

    @Test
    @DisplayName("detachFiles는 파일 분리를 파일 애플리케이션에 위임한다")
    void detachFiles_delegatesToFileAttachmentService() {
        Post post = post(1L);

        postFileAppender.detachFiles(post);

        verify(fileAttachmentService).detachFiles(post);
    }

    private Post post(Long postId) {
        return Post.builder()
            .id(postId)
            .title("title")
            .content("content")
            .accountId(1L)
            .nickname("writer")
            .build();
    }

}
