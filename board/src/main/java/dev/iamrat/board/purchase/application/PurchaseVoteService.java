package dev.iamrat.board.purchase.application;

import dev.iamrat.board.post.application.PostReader;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.purchase.domain.PostPurchaseVote;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseVoteService {

    private final PostReader postReader;
    private final PurchaseVoteStore purchaseVoteStore;
    private final PurchaseVoteEligibility purchaseVoteEligibility;
    private final PurchaseVoteQueryService purchaseVoteQueryService;

    @Transactional
    public PurchaseVoteSummary vote(Long postId, Long accountId, PurchaseVoteType voteType) {
        validate(accountId, voteType);
        Post post = eligiblePost(postId);
        purchaseVoteStore.findByPostIdAndAccountId(postId, accountId)
            .ifPresentOrElse(
                existing -> existing.updateVote(voteType),
                () -> purchaseVoteStore.save(PostPurchaseVote.of(post, accountId, voteType))
            );
        return purchaseVoteQueryService.getSummary(post, accountId);
    }

    @Transactional
    public PurchaseVoteSummary unvote(Long postId, Long accountId) {
        if (accountId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
        Post post = eligiblePost(postId);
        purchaseVoteStore.deleteByPostIdAndAccountId(postId, accountId);
        return purchaseVoteQueryService.getSummary(post, accountId);
    }

    private void validate(Long accountId, PurchaseVoteType voteType) {
        if (accountId == null || voteType == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private Post eligiblePost(Long postId) {
        Post post = postReader.getById(postId);
        if (!purchaseVoteEligibility.isEligible(post)) {
            throw new CustomException(BoardErrorCode.PURCHASE_VOTE_NOT_ALLOWED);
        }
        return post;
    }
}
