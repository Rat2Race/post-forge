package dev.iamrat.candidate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "crawl.candidate")
public class CandidateConfig {
    private int disclosureLookbackHours = 18;
    private double priceChangeThreshold = 3.0;
    private double volumeRatioThreshold = 2.0;
    private long tradingValueThreshold = 30_000_000_000L;
    private int topTradingValueRankThreshold = 20;
    private int newsBurstThreshold = 3;
    private int themeKeywordMinHits = 2;
    private long largeCapMarketCapThreshold = 10_000_000_000_000L;
}

