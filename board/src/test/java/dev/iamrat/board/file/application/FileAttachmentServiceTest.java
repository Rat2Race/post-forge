package dev.iamrat.board.file.application;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FileAttachmentServiceTest {

    @Mock
    private FileStore fileStore;

    @InjectMocks
    private FileAttachmentService fileAttachmentService;

    @Test
    @DisplayName("appendFiles assigns requested files to the post")
    void appendFiles_assignsRequestedFilesToPost() {
        Post post = post(1L);
        PostFile file = file(10L, null);
        given(fileStore.findAllByIdIn(List.of(10L))).willReturn(List.of(file));

        fileAttachmentService.appendFiles(post, List.of(10L));

        assertThat(file.getPost()).isSameAs(post);
    }

    @Test
    @DisplayName("appendFiles ignores empty file IDs")
    void appendFiles_whenFileIdsEmpty_doesNothing() {
        fileAttachmentService.appendFiles(post(1L), List.of());

        verifyNoInteractions(fileStore);
    }

    @Test
    @DisplayName("replaceFiles detaches existing files before assigning new files")
    void replaceFiles_detachesExistingFilesBeforeAssigningNewFiles() {
        Post post = post(1L);
        PostFile oldFile = file(10L, post);
        PostFile newFile = file(20L, null);
        given(fileStore.findAllByPost(post)).willReturn(List.of(oldFile));
        given(fileStore.findAllByIdIn(List.of(20L))).willReturn(List.of(newFile));

        fileAttachmentService.replaceFiles(post, List.of(20L));

        assertThat(oldFile.getPost()).isNull();
        assertThat(newFile.getPost()).isSameAs(post);
    }

    @Test
    @DisplayName("detachFiles unassigns every file attached to the post")
    void detachFiles_unassignsAttachedFiles() {
        Post post = post(1L);
        PostFile file = file(10L, post);
        given(fileStore.findAllByPost(post)).willReturn(List.of(file));

        fileAttachmentService.detachFiles(post);

        assertThat(file.getPost()).isNull();
        verify(fileStore).findAllByPost(post);
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

    private PostFile file(Long fileId, Post post) {
        return PostFile.builder()
            .id(fileId)
            .originalFileName("photo.png")
            .savedFileName(fileId + ".png")
            .filePath("images/" + fileId + ".png")
            .fileType("image/png")
            .post(post)
            .build();
    }
}
