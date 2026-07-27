package dev.iamrat.board.purchase.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.purchase.application.PurchaseVoteCount;
import dev.iamrat.board.purchase.domain.PostPurchaseVote;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseVotePersistenceAdapterTest {

    @Mock
    private PurchaseVoteRepository repository;

    @InjectMocks
    private PurchaseVotePersistenceAdapter adapter;

    @Test
    @DisplayName("투표 영속성을 위임하고 투표 수를 매핑한다")
    void delegatesVotePersistenceAndMapsCounts() {
        Post post = Post.builder().id(1L).title("title").content("content").nickname("writer").build();
        PostPurchaseVote vote = PostPurchaseVote.of(post, 2L, PurchaseVoteType.BUYABLE);
        List<Object[]> rows = List.<Object[]>of(new Object[] {1L, PurchaseVoteType.BUYABLE, 3L});

        given(repository.findByPost_IdAndAccountId(1L, 2L)).willReturn(Optional.of(vote));
        given(repository.findByAccountIdAndPost_IdIn(2L, List.of(1L))).willReturn(List.of(vote));
        given(repository.save(vote)).willReturn(vote);
        given(repository.deleteByPost_IdAndAccountId(1L, 2L)).willReturn(1L);
        given(repository.countByPostIds(List.of(1L))).willReturn(rows);

        assertThat(adapter.findByPostIdAndAccountId(1L, 2L)).contains(vote);
        assertThat(adapter.findByAccountIdAndPostIds(2L, List.of(1L))).containsExactly(vote);
        assertThat(adapter.save(vote)).isSameAs(vote);
        assertThat(adapter.deleteByPostIdAndAccountId(1L, 2L)).isEqualTo(1L);
        assertThat(adapter.countByPostIds(List.of(1L)))
            .containsExactly(new PurchaseVoteCount(1L, PurchaseVoteType.BUYABLE, 3L));
    }
}
