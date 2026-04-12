package dev.iamrat.crawl.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_master")
@NoArgsConstructor
public class StockMaster {

    @Id
    @Column(length = 10)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String englishName;

    @Column(nullable = false, length = 20)
    private String market;

    @Column(length = 1000)
    private String aliases;

    private Long marketCap;

    @Column(length = 20)
    private String dartCorpCode;

    private boolean isEtf = false;

    private boolean isPreferred = false;

    private boolean isSpac = false;

    private boolean isSuspended = false;

    private LocalDateTime updatedAt;

    public StockMaster(
        String ticker,
        String name,
        String englishName,
        String market,
        String aliases,
        Long marketCap,
        String dartCorpCode,
        boolean isEtf,
        boolean isPreferred,
        boolean isSpac,
        boolean isSuspended,
        LocalDateTime updatedAt
    ) {
        this.ticker = ticker;
        this.name = name;
        this.englishName = englishName;
        this.market = market;
        this.aliases = aliases;
        this.marketCap = marketCap;
        this.dartCorpCode = dartCorpCode;
        this.isEtf = isEtf;
        this.isPreferred = isPreferred;
        this.isSpac = isSpac;
        this.isSuspended = isSuspended;
        this.updatedAt = updatedAt;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getMarket() {
        return market;
    }

    public String getAliases() {
        return aliases;
    }

    public Long getMarketCap() {
        return marketCap;
    }

    public String getDartCorpCode() {
        return dartCorpCode;
    }

    public boolean isEtf() {
        return isEtf;
    }

    public boolean isPreferred() {
        return isPreferred;
    }

    public boolean isSpac() {
        return isSpac;
    }

    public boolean isSuspended() {
        return isSuspended;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
