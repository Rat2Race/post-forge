package dev.iamrat.board.purchase.application;

import dev.iamrat.board.purchase.domain.PostPurchaseVote;
import java.util.List;
import java.util.Optional;

public interface PurchaseVoteStore {

    Optional<PostPurchaseVote> findByPostIdAndAccountId(Long postId, Long accountId);

    List<PostPurchaseVote> findByAccountIdAndPostIds(Long accountId, List<Long> postIds);

    PostPurchaseVote save(PostPurchaseVote vote);

    long deleteByPostIdAndAccountId(Long postId, Long accountId);

    List<PurchaseVoteCount> countByPostIds(List<Long> postIds);
}
