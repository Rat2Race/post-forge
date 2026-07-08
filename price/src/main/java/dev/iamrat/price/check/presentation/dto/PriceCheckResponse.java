package dev.iamrat.price.check.presentation.dto;

import dev.iamrat.price.check.application.PriceCheckConfidence;
import dev.iamrat.price.check.application.PriceCheckResult;
import dev.iamrat.price.check.application.PriceJudgement;
import java.util.List;

public record PriceCheckResponse(
    PriceJudgement judgement,
    PriceCheckConfidence confidence,
    long candidateEffectivePrice,
    boolean shippingIncludedVerified,
    String message,
    PriceCheckBasisResponse basis,
    List<PriceCheckItemResponse> items
) {
    public static PriceCheckResponse from(PriceCheckResult result) {
        return new PriceCheckResponse(
            result.judgement(),
            result.confidence(),
            result.candidateEffectivePrice(),
            result.shippingIncludedVerified(),
            result.message(),
            PriceCheckBasisResponse.from(result.basis()),
            result.items().stream()
                .map(PriceCheckItemResponse::from)
                .toList()
        );
    }
}
