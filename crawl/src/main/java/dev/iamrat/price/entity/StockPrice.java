package dev.iamrat.crawl.price.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "stock_prices",
    uniqueConstraints = @UniqueConstraint(name = "uk_stock_prices_ticker_trade_date", columnNames = {"ticker", "tradeDate"})
)
@NoArgsConstructor
public class StockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(length = 100)
    private String stockName;

    @Column(length = 20)
    private String market;

    @Column(nullable = false)
    private LocalDate tradeDate;

    private Long closePrice;
    private Long openPrice;
    private Long highPrice;
    private Long lowPrice;
    private Long volume;
    private Long tradingValue;

    @Column(precision = 8, scale = 4)
    private BigDecimal changeRate;

    private Long avgVolume20d;
    private Long foreignNetBuy;
    private Long instNetBuy;
    private LocalDateTime collectedAt;

    public StockPrice(
        Long id,
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
        BigDecimal changeRate,
        Long avgVolume20d,
        Long foreignNetBuy,
        Long instNetBuy,
        LocalDateTime collectedAt
    ) {
        this.id = id;
        this.ticker = ticker;
        this.stockName = stockName;
        this.market = market;
        this.tradeDate = tradeDate;
        this.closePrice = closePrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.tradingValue = tradingValue;
        this.changeRate = changeRate;
        this.avgVolume20d = avgVolume20d;
        this.foreignNetBuy = foreignNetBuy;
        this.instNetBuy = instNetBuy;
        this.collectedAt = collectedAt;
    }

    public void applySnapshot(
        String stockName,
        String market,
        Long closePrice,
        Long openPrice,
        Long highPrice,
        Long lowPrice,
        Long volume,
        Long tradingValue,
        BigDecimal changeRate,
        Long avgVolume20d
    ) {
        this.stockName = stockName;
        this.market = market;
        this.closePrice = closePrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.tradingValue = tradingValue;
        this.changeRate = changeRate;
        this.avgVolume20d = avgVolume20d;
        this.collectedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        collectedAt = LocalDateTime.now();
    }
}
