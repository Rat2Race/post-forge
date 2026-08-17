package dev.iamrat.board.purchase.application;

import dev.iamrat.board.purchase.domain.PurchaseVoteType;

public record PurchaseVoteSummary(
    Long postId,
    boolean eligible,
    long buyableCount,
    long unsureCount,
    long waitCount,
    PurchaseVoteType myVote
) {
    public static PurchaseVoteSummary empty(Long postId, boolean eligible) {
        return new PurchaseVoteSummary(postId, eligible, 0L, 0L, 0L, null);
    }
}
