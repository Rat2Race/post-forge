package dev.iamrat.board.purchase.application;

import dev.iamrat.board.purchase.domain.PurchaseVoteType;

public record PurchaseVoteCount(
    Long postId,
    PurchaseVoteType voteType,
    long count
) {
}
