package dev.iamrat.board.purchase.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import org.springframework.stereotype.Component;

@Component
public class PurchaseVoteEligibility {

    public boolean isEligible(Post post) {
        return post != null
            && post.getCategory() == PostCategory.PRODUCT_LAUNCH_NEWS
            && post.getPublishOrigin() == PostPublishOrigin.SYSTEM_BATCH;
    }
}
