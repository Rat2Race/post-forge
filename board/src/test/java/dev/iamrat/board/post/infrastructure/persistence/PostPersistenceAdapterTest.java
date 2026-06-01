package dev.iamrat.board.post.infrastructure.persistence;

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
class PostPersistenceAdapterTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostPersistenceAdapter postPersistenceAdapter;

    @Test
    @DisplayName("save는 Spring Data repository에 위임한다")
    void save_delegatesToRepository() {
        Post post = post(1L);
        given(postRepository.save(post)).willReturn(post);

        Post result = postPersistenceAdapter.save(post);

        assertThat(result).isSameAs(post);
    }

    @Test
    @DisplayName("findById는 Spring Data repository에 위임한다")
    void findById_delegatesToRepository() {
        Post post = post(1L);
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        Optional<Post> result = postPersistenceAdapter.findById(1L);

        assertThat(result).containsSame(post);
    }

    @Test
    @DisplayName("findAll은 Spring Data repository에 위임한다")
    void findAll_delegatesToRepository() {
        PageRequest pageable = PageRequest.of(0, 10);
        Post post = post(1L);
        given(postRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(post), pageable, 1));

        Page<Post> result = postPersistenceAdapter.findAll(pageable);

        assertThat(result.getContent()).containsExactly(post);
    }

    @Test
    @DisplayName("findByKeyword는 Spring Data repository에 위임한다")
    void findByKeyword_delegatesToRepository() {
        PageRequest pageable = PageRequest.of(0, 10);
        Post post = post(1L);
        given(postRepository.findByKeyword("keyword", pageable))
            .willReturn(new PageImpl<>(List.of(post), pageable, 1));

        Page<Post> result = postPersistenceAdapter.findByKeyword("keyword", pageable);

        assertThat(result.getContent()).containsExactly(post);
    }

    @Test
    @DisplayName("카운터 갱신은 Spring Data repository에 위임한다")
    void updateCounters_delegateToRepository() {
        postPersistenceAdapter.updateViews(1L, 10L);
        postPersistenceAdapter.updateLikeCount(1L, 3L);

        verify(postRepository).updateViews(1L, 10L);
        verify(postRepository).updateLikeCount(1L, 3L);
    }

    @Test
    @DisplayName("delete는 Spring Data repository에 위임한다")
    void delete_delegatesToRepository() {
        Post post = post(1L);

        postPersistenceAdapter.delete(post);

        verify(postRepository).delete(post);
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
