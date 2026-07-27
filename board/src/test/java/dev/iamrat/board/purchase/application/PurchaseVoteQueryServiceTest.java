package dev.iamrat.board.purchase.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.purchase.domain.PostPurchaseVote;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseVoteQueryServiceTest {

    @Mock
    private PurchaseVoteStore purchaseVoteStore;

    @Spy
    private PurchaseVoteEligibility purchaseVoteEligibility = new PurchaseVoteEligibility();

    @InjectMocks
    private PurchaseVoteQueryService service;

    @Test
    @DisplayName("구매 투표 요약 조회는 대상 게시글의 집계와 내 투표만 반환한다")
    void getSummaries_returnsCountsAndMyVoteOnlyForEligiblePosts() {
        Post eligible = post(1L, PostCategory.PRODUCT_LAUNCH_NEWS, PostPublishOrigin.SYSTEM_BATCH);
        Post ineligible = post(2L, PostCategory.PRODUCT_LAUNCH_NEWS, PostPublishOrigin.ADMIN_BACKFILL);
        given(purchaseVoteStore.countByPostIds(List.of(1L))).willReturn(List.of(
            new PurchaseVoteCount(1L, PurchaseVoteType.BUYABLE, 2L),
            new PurchaseVoteCount(1L, PurchaseVoteType.WAIT, 1L)
        ));
        given(purchaseVoteStore.findByAccountIdAndPostIds(7L, List.of(1L))).willReturn(List.of(
            PostPurchaseVote.of(eligible, 7L, PurchaseVoteType.WAIT)
        ));

        Map<Long, PurchaseVoteSummary> summaries = service.getSummaries(List.of(eligible, ineligible), 7L);

        assertThat(summaries.get(1L).eligible()).isTrue();
        assertThat(summaries.get(1L).buyableCount()).isEqualTo(2L);
        assertThat(summaries.get(1L).unsureCount()).isZero();
        assertThat(summaries.get(1L).waitCount()).isEqualTo(1L);
        assertThat(summaries.get(1L).myVote()).isEqualTo(PurchaseVoteType.WAIT);

        assertThat(summaries.get(2L).eligible()).isFalse();
        assertThat(summaries.get(2L).buyableCount()).isZero();
        assertThat(summaries.get(2L).myVote()).isNull();
    }

    private Post post(Long id, PostCategory category, PostPublishOrigin publishOrigin) {
        return Post.builder()
            .id(id)
            .title("title")
            .content("content")
            .category(category)
            .publishOrigin(publishOrigin)
            .accountId(1L)
            .nickname("writer")
            .build();
    }
}
