package dev.iamrat.board.purchase.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.board.post.application.PostReader;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.purchase.domain.PostPurchaseVote;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseVoteServiceTest {

    @Mock
    private PostReader postReader;

    @Mock
    private PurchaseVoteStore purchaseVoteStore;

    @Spy
    private PurchaseVoteEligibility purchaseVoteEligibility = new PurchaseVoteEligibility();

    @Mock
    private PurchaseVoteQueryService purchaseVoteQueryService;

    @InjectMocks
    private PurchaseVoteService service;

    @Test
    @DisplayName("구매 투표 대상 게시글에는 새 투표를 생성한다")
    void vote_createsVoteForEligiblePost() {
        Post post = eligiblePost();
        given(postReader.getById(1L)).willReturn(post);
        given(purchaseVoteStore.findByPostIdAndAccountId(1L, 9L)).willReturn(Optional.empty());
        given(purchaseVoteQueryService.getSummary(post, 9L)).willReturn(summary(PurchaseVoteType.BUYABLE));

        PurchaseVoteSummary summary = service.vote(1L, 9L, PurchaseVoteType.BUYABLE);

        ArgumentCaptor<PostPurchaseVote> captor = ArgumentCaptor.forClass(PostPurchaseVote.class);
        verify(purchaseVoteStore).save(captor.capture());
        assertThat(captor.getValue().getPost()).isSameAs(post);
        assertThat(captor.getValue().getAccountId()).isEqualTo(9L);
        assertThat(captor.getValue().getVoteType()).isEqualTo(PurchaseVoteType.BUYABLE);
        assertThat(summary.myVote()).isEqualTo(PurchaseVoteType.BUYABLE);
    }

    @Test
    @DisplayName("이미 투표한 게시글에는 기존 투표를 갱신한다")
    void vote_updatesExistingVote() {
        Post post = eligiblePost();
        PostPurchaseVote existing = PostPurchaseVote.of(post, 9L, PurchaseVoteType.UNSURE);
        given(postReader.getById(1L)).willReturn(post);
        given(purchaseVoteStore.findByPostIdAndAccountId(1L, 9L)).willReturn(Optional.of(existing));
        given(purchaseVoteQueryService.getSummary(post, 9L)).willReturn(summary(PurchaseVoteType.WAIT));

        service.vote(1L, 9L, PurchaseVoteType.WAIT);

        assertThat(existing.getVoteType()).isEqualTo(PurchaseVoteType.WAIT);
    }

    @Test
    @DisplayName("구매 투표 대상이 아닌 게시글은 거절한다")
    void vote_rejectsIneligiblePosts() {
        given(postReader.getById(1L)).willReturn(Post.create(
            "title",
            "content",
            null,
            null,
            PostCategory.PRODUCT_LAUNCH_NEWS,
            PostPublishOrigin.ADMIN_BACKFILL,
            1L,
            "writer"
        ));

        assertThatThrownBy(() -> service.vote(1L, 9L, PurchaseVoteType.BUYABLE))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("구매 투표 취소는 투표를 삭제하고 요약을 반환한다")
    void unvote_deletesVoteAndReturnsSummary() {
        Post post = eligiblePost();
        given(postReader.getById(1L)).willReturn(post);
        given(purchaseVoteQueryService.getSummary(post, 9L)).willReturn(PurchaseVoteSummary.empty(1L, true));

        PurchaseVoteSummary summary = service.unvote(1L, 9L);

        verify(purchaseVoteStore).deleteByPostIdAndAccountId(1L, 9L);
        assertThat(summary.myVote()).isNull();
    }

    private Post eligiblePost() {
        return Post.create(
            "launch",
            "content",
            null,
            null,
            PostCategory.PRODUCT_LAUNCH_NEWS,
            PostPublishOrigin.SYSTEM_BATCH,
            0L,
            "system"
        );
    }

    private PurchaseVoteSummary summary(PurchaseVoteType myVote) {
        return new PurchaseVoteSummary(1L, true, 1L, 0L, 0L, myVote);
    }
}
