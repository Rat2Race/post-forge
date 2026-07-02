package dev.iamrat.board.purchase.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.board.post.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostPurchaseVoteTest {

    @Test
    @DisplayName("구매 투표를 생성하고 선택지를 변경할 수 있다")
    void of_createsVoteAndUpdateVoteChangesChoice() {
        Post post = Post.builder().id(1L).title("title").content("content").nickname("writer").build();

        PostPurchaseVote vote = PostPurchaseVote.of(post, 2L, PurchaseVoteType.BUYABLE);

        assertThat(vote.getPost()).isSameAs(post);
        assertThat(vote.getAccountId()).isEqualTo(2L);
        assertThat(vote.getVoteType()).isEqualTo(PurchaseVoteType.BUYABLE);

        vote.updateVote(PurchaseVoteType.WAIT);

        assertThat(vote.getVoteType()).isEqualTo(PurchaseVoteType.WAIT);
    }
}
