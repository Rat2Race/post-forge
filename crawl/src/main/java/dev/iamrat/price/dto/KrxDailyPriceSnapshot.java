package dev.iamrat.crawl.price.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record KrxDailyPriceSnapshot(
    String ticker,
    String stockName,
    String market,
    LocalDate tradeDate,
    Long closePrice,
    Long openPrice,
    Long highPrice,
    Long lowPrice,
    Long volume,
    Long tradingValue,
    BigDecimal changeRate
) {
}
