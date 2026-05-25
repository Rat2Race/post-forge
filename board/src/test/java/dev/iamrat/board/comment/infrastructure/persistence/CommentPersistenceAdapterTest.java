package dev.iamrat.board.comment.infrastructure.persistence;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentPersistenceAdapterTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentPersistenceAdapter commentPersistenceAdapter;

    @Test
    @DisplayName("save delegates to the Spring Data repository")
    void save_delegatesToRepository() {
        Comment comment = comment(1L);
        given(commentRepository.save(comment)).willReturn(comment);

        Comment result = commentPersistenceAdapter.save(comment);

        assertThat(result).isSameAs(comment);
    }

    @Test
    @DisplayName("findById delegates to the Spring Data repository")
    void findById_delegatesToRepository() {
        Comment comment = comment(1L);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        Optional<Comment> result = commentPersistenceAdapter.findById(1L);

        assertThat(result).containsSame(comment);
    }

    @Test
    @DisplayName("findByPostId delegates to the Spring Data repository")
    void findByPostId_delegatesToRepository() {
        PageRequest pageable = PageRequest.of(0, 10);
        Comment comment = comment(1L);
        given(commentRepository.findByPostId(1L, pageable))
            .willReturn(new PageImpl<>(List.of(comment), pageable, 1));

        Page<Comment> result = commentPersistenceAdapter.findByPostId(1L, pageable);

        assertThat(result.getContent()).containsExactly(comment);
    }

    @Test
    @DisplayName("countByPostIds delegates to the Spring Data repository")
    void countByPostIds_delegatesToRepository() {
        List<Object[]> rows = List.<Object[]>of(new Object[]{1L, 2L});
        given(commentRepository.countByPostIds(List.of(1L))).willReturn(rows);

        List<Object[]> result = commentPersistenceAdapter.countByPostIds(List.of(1L));

        assertThat(result).isSameAs(rows);
    }

    @Test
    @DisplayName("updateLikeCount delegates to the Spring Data repository")
    void updateLikeCount_delegatesToRepository() {
        commentPersistenceAdapter.updateLikeCount(1L, 3L);

        verify(commentRepository).updateLikeCount(1L, 3L);
    }

    @Test
    @DisplayName("delete delegates to the Spring Data repository")
    void delete_delegatesToRepository() {
        Comment comment = comment(1L);

        commentPersistenceAdapter.delete(comment);

        verify(commentRepository).delete(comment);
    }

    private Comment comment(Long commentId) {
        return Comment.builder()
            .id(commentId)
            .post(Post.builder().id(1L).title("title").content("content").accountId(1L).build())
            .content("comment")
            .accountId(2L)
            .nickname("writer")
            .build();
    }
}
