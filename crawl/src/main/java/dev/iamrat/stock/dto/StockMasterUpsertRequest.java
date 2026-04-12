package dev.iamrat.crawl.stock.dto;

import jakarta.validation.constraints.NotBlank;

public record StockMasterUpsertRequest(
    @NotBlank String ticker,
    @NotBlank String name,
    String englishName,
    @NotBlank String market,
    String aliases,
    Long marketCap,
    String dartCorpCode,
    boolean isEtf,
    boolean isPreferred,
    boolean isSpac,
    boolean isSuspended
) {
}
