package dev.iamrat.board.purchase.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurchaseVoteEligibilityTest {

    private final PurchaseVoteEligibility eligibility = new PurchaseVoteEligibility();

    @Test
    @DisplayName("시스템 배치 상품 출시 뉴스만 구매 투표 대상이다")
    void eligibleOnlyForSystemBatchProductLaunchNews() {
        Post eligiblePost = post(PostCategory.PRODUCT_LAUNCH_NEWS, PostPublishOrigin.SYSTEM_BATCH);
        Post backfillPost = post(PostCategory.PRODUCT_LAUNCH_NEWS, PostPublishOrigin.ADMIN_BACKFILL);
        Post generalPost = post(PostCategory.GENERAL, PostPublishOrigin.SYSTEM_BATCH);

        assertThat(eligibility.isEligible(eligiblePost)).isTrue();
        assertThat(eligibility.isEligible(backfillPost)).isFalse();
        assertThat(eligibility.isEligible(generalPost)).isFalse();
        assertThat(eligibility.isEligible(null)).isFalse();
    }

    private Post post(PostCategory category, PostPublishOrigin publishOrigin) {
        return Post.create("title", "content", null, null, category, publishOrigin, 1L, "writer");
    }
}
