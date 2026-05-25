package dev.iamrat.board.file.infrastructure.persistence;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FilePersistenceAdapterTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FilePersistenceAdapter filePersistenceAdapter;

    @Test
    @DisplayName("save delegates to the Spring Data repository")
    void save_delegatesToRepository() {
        PostFile file = file(1L, null);
        given(fileRepository.save(file)).willReturn(file);

        PostFile result = filePersistenceAdapter.save(file);

        assertThat(result).isSameAs(file);
    }

    @Test
    @DisplayName("findById delegates to the Spring Data repository")
    void findById_delegatesToRepository() {
        PostFile file = file(1L, null);
        given(fileRepository.findById(1L)).willReturn(Optional.of(file));

        Optional<PostFile> result = filePersistenceAdapter.findById(1L);

        assertThat(result).containsSame(file);
    }

    @Test
    @DisplayName("findAllByIdIn delegates to the Spring Data repository")
    void findAllByIdIn_delegatesToRepository() {
        PostFile file = file(1L, null);
        given(fileRepository.findAllByIdIn(List.of(1L))).willReturn(List.of(file));

        List<PostFile> result = filePersistenceAdapter.findAllByIdIn(List.of(1L));

        assertThat(result).containsExactly(file);
    }

    @Test
    @DisplayName("findAllByPost delegates to the Spring Data repository")
    void findAllByPost_delegatesToRepository() {
        Post post = post(1L);
        PostFile file = file(1L, post);
        given(fileRepository.findAllByPost(post)).willReturn(List.of(file));

        List<PostFile> result = filePersistenceAdapter.findAllByPost(post);

        assertThat(result).containsExactly(file);
    }

    @Test
    @DisplayName("deleteOrphanFilesBefore delegates to the Spring Data repository")
    void deleteOrphanFilesBefore_delegatesToRepository() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        given(fileRepository.deleteOrphanFiles(threshold)).willReturn(3);

        int result = filePersistenceAdapter.deleteOrphanFilesBefore(threshold);

        assertThat(result).isEqualTo(3);
        verify(fileRepository).deleteOrphanFiles(threshold);
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
