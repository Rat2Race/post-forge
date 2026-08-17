package dev.iamrat.board.purchase.presentation;

import dev.iamrat.board.purchase.application.PurchaseVoteSummary;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;

public record PurchaseVoteResponse(
    Long postId,
    boolean eligible,
    long buyableCount,
    long unsureCount,
    long waitCount,
    PurchaseVoteType myVote
) {
    public static PurchaseVoteResponse from(PurchaseVoteSummary summary) {
        return new PurchaseVoteResponse(
            summary.postId(),
            summary.eligible(),
            summary.buyableCount(),
            summary.unsureCount(),
            summary.waitCount(),
            summary.myVote()
        );
    }

    public static PurchaseVoteResponse ineligible(Long postId) {
        return new PurchaseVoteResponse(postId, false, 0L, 0L, 0L, null);
    }
}
