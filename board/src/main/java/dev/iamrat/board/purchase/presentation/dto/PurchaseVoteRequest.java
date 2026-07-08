package dev.iamrat.board.purchase.presentation.dto;

import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import jakarta.validation.constraints.NotNull;

public record PurchaseVoteRequest(
    @NotNull PurchaseVoteType voteType
) {
}
