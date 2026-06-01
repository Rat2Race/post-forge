package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.like.domain.PostLike;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import java.util.Set;
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
class PostLikePersistenceAdapterTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostLikePersistenceAdapter postLikePersistenceAdapter;

    @Test
    @DisplayName("existsByPostIdAndAccountId는 Spring Data repository에 위임한다")
    void existsByPostIdAndAccountId_delegatesToRepository() {
        given(postLikeRepository.existsByPost_IdAndAccountId(1L, 2L)).willReturn(true);

        boolean result = postLikePersistenceAdapter.existsByPostIdAndAccountId(1L, 2L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("save는 Spring Data repository에 위임한다")
    void save_delegatesToRepository() {
        PostLike postLike = PostLike.of(post(1L), 2L);
        given(postLikeRepository.save(postLike)).willReturn(postLike);

        PostLike result = postLikePersistenceAdapter.save(postLike);

        assertThat(result).isSameAs(postLike);
    }

    @Test
    @DisplayName("count와 delete는 Spring Data repository에 위임한다")
    void countAndDelete_delegateToRepository() {
        given(postLikeRepository.countByPost_Id(1L)).willReturn(3L);
        given(postLikeRepository.deleteByPost_IdAndAccountId(1L, 2L)).willReturn(1L);

        assertThat(postLikePersistenceAdapter.countByPostId(1L)).isEqualTo(3L);
        assertThat(postLikePersistenceAdapter.deleteByPostIdAndAccountId(1L, 2L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("bulk 조회는 Spring Data repository에 위임한다")
    void bulkQueries_delegateToRepository() {
        List<Object[]> rows = List.<Object[]>of(new Object[]{1L, 2L});
        given(postLikeRepository.countByPostIds(List.of(1L))).willReturn(rows);
        given(postLikeRepository.findLikedPostIdsByAccountIdAndPostIds(2L, List.of(1L))).willReturn(Set.of(1L));

        assertThat(postLikePersistenceAdapter.countByPostIds(List.of(1L))).isSameAs(rows);
        assertThat(postLikePersistenceAdapter.findLikedPostIdsByAccountIdAndPostIds(2L, List.of(1L)))
            .containsExactly(1L);
    }

    @Test
    @DisplayName("삭제 갱신 경로는 repository를 호출한다")
    void deleteByPostIdAndAccountId_invokesRepository() {
        postLikePersistenceAdapter.deleteByPostIdAndAccountId(1L, 2L);

        verify(postLikeRepository).deleteByPost_IdAndAccountId(1L, 2L);
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
