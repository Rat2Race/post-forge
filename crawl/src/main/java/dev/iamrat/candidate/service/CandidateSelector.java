package dev.iamrat.candidate.service;

import dev.iamrat.candidate.config.CandidateConfig;
import dev.iamrat.candidate.entity.CandidateSelection;
import dev.iamrat.candidate.repository.CandidateSelectionRepository;
import dev.iamrat.common.entity.CrawledArticle;
import dev.iamrat.common.repository.CrawledArticleRepository;
import dev.iamrat.price.entity.StockPrice;
import dev.iamrat.price.repository.StockPriceRepository;
import dev.iamrat.stock.entity.StockMaster;
import dev.iamrat.stock.repository.StockMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CandidateSelector {

    private static final String SOURCE_DART = "dart";
    private static final String SOURCE_NAVER_NEWS = "naver-news";

    private final CandidateSelectionRepository candidateSelectionRepository;
    private final StockMasterRepository stockMasterRepository;
    private final StockPriceRepository stockPriceRepository;
    private final CrawledArticleRepository crawledArticleRepository;
    private final CandidateConfig candidateConfig;
    private final Clock clock;

    @Transactional
    public List<CandidateSelection> select(LocalDate runDate) {
        candidateSelectionRepository.deleteByRunDate(runDate);

        SelectionWindow window = resolveSelectionWindow(runDate);
        List<StockMaster> stocks = stockMasterRepository
            .findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc();
        Map<String, StockPrice> latestPrices = resolveLatestPrices(stocks, runDate);
        Map<String, Integer> tradingValueRanks = resolveTradingValueRanks(latestPrices);
        List<CrawledArticle> recentNewsArticles = crawledArticleRepository.findBySourceAndCrawledAtBetween(
            SOURCE_NAVER_NEWS,
            window.start(),
            window.end()
        );

        List<CandidateSelection> selections = new ArrayList<>();
        for (StockMaster stock : stocks) {
            CandidateSelection selection = buildSelection(runDate, window, stock, latestPrices, tradingValueRanks, recentNewsArticles);
            if (selection != null) {
                selections.add(candidateSelectionRepository.save(selection));
            }
        }

        return selections;
    }

    @Transactional(readOnly = true)
    public List<CandidateSelection> getSelections(LocalDate runDate) {
        return candidateSelectionRepository.findByRunDateOrderByCreatedAtAsc(runDate);
    }

    private CandidateSelection buildSelection(
        LocalDate runDate,
        SelectionWindow window,
        StockMaster stock,
        Map<String, StockPrice> latestPrices,
        Map<String, Integer> tradingValueRanks,
        List<CrawledArticle> recentNewsArticles
    ) {
        StockPrice latestPrice = latestPrices.get(stock.getTicker());
        int disclosureCount = countArticles(SOURCE_DART, stock.getTicker(), window);
        NewsSignal newsSignal = analyzeNewsSignal(stock, recentNewsArticles);
        BigDecimal priceChange = latestPrice != null && latestPrice.getChangeRate() != null
            ? latestPrice.getChangeRate()
            : BigDecimal.ZERO;
        BigDecimal volumeRatio = calculateVolumeRatio(latestPrice);
        long tradingValue = latestPrice != null && latestPrice.getTradingValue() != null
            ? latestPrice.getTradingValue()
            : 0L;
        long marketCap = stock.getMarketCap() != null ? stock.getMarketCap() : 0L;
        boolean largeCap = isLargeCap(stock);
        int tradingValueRank = tradingValueRanks.getOrDefault(stock.getTicker(), Integer.MAX_VALUE);

        List<String> reasons = buildReasons(
            disclosureCount,
            newsSignal,
            priceChange,
            volumeRatio,
            tradingValue,
            tradingValueRank,
            largeCap
        );
        if (reasons.isEmpty()) {
            return null;
        }

        return new CandidateSelection(
            null,
            runDate,
            stock.getTicker(),
            stock.getName(),
            String.join(", ", reasons),
            priceChange,
            volumeRatio,
            tradingValue,
            marketCap,
            largeCap,
            disclosureCount,
            newsSignal.newsCount(),
            newsSignal.themeKeywordHits(),
            null
        );
    }

    private int countArticles(String source, String keyword, SelectionWindow window) {
        return (int) crawledArticleRepository.countBySourceAndKeywordAndCrawledAtBetween(
            source,
            keyword,
            window.start(),
            window.end()
        );
    }

    private NewsSignal analyzeNewsSignal(StockMaster stock, List<CrawledArticle> recentNewsArticles) {
        if (recentNewsArticles == null || recentNewsArticles.isEmpty()) {
            return NewsSignal.EMPTY;
        }

        int stockNewsCount = 0;
        int themeKeywordHits = 0;
        List<String> aliasKeywords = parseAliasKeywords(stock.getAliases());

        for (CrawledArticle article : recentNewsArticles) {
            String title = normalize(article.getTitle());
            if (title.isBlank()) {
                continue;
            }
            if (title.contains(normalize(stock.getName()))) {
                stockNewsCount++;
            }
            if (!aliasKeywords.isEmpty() && aliasKeywords.stream().anyMatch(title::contains)) {
                themeKeywordHits++;
            }
        }

        return new NewsSignal(stockNewsCount, themeKeywordHits);
    }

    private List<String> buildReasons(
        int disclosureCount,
        NewsSignal newsSignal,
        BigDecimal priceChange,
        BigDecimal volumeRatio,
        long tradingValue,
        int tradingValueRank,
        boolean largeCap
    ) {
        List<String> reasons = new ArrayList<>();

        if (tradingValue >= candidateConfig.getTradingValueThreshold() && tradingValueRank <= candidateConfig.getTopTradingValueRankThreshold()) {
            reasons.add("거래대금 상위 " + tradingValueRank + "위권");
        }
        if (tradingValue >= candidateConfig.getTradingValueThreshold()) {
            reasons.add("거래대금 " + formatWon(tradingValue));
        }
        if (priceChange.compareTo(BigDecimal.valueOf(candidateConfig.getPriceChangeThreshold())) >= 0) {
            reasons.add("급등 " + priceChange.toPlainString() + "%");
        }
        if (priceChange.compareTo(BigDecimal.valueOf(-candidateConfig.getPriceChangeThreshold())) <= 0) {
            reasons.add("급락 " + priceChange.toPlainString() + "%");
        }

        BigDecimal volumeThreshold = BigDecimal.valueOf(candidateConfig.getVolumeRatioThreshold()).multiply(BigDecimal.valueOf(100));
        if (volumeRatio.compareTo(volumeThreshold) >= 0) {
            reasons.add("거래량 급증 " + volumeRatio.toPlainString() + "%");
        }
        if (newsSignal.newsCount() >= candidateConfig.getNewsBurstThreshold()) {
            reasons.add("뉴스 집중 " + newsSignal.newsCount() + "건");
        }
        if (newsSignal.themeKeywordHits() >= candidateConfig.getThemeKeywordMinHits()) {
            reasons.add("테마 키워드 반응 " + newsSignal.themeKeywordHits() + "건");
        }
        if (disclosureCount > 0) {
            reasons.add("DART 공시 " + disclosureCount + "건 감지");
        }
        if (largeCap && hasMarketSignal(tradingValue, priceChange, volumeRatio, newsSignal)) {
            reasons.add("대형주 시그널");
        }
        return reasons;
    }

    private boolean hasMarketSignal(long tradingValue, BigDecimal priceChange, BigDecimal volumeRatio, NewsSignal newsSignal) {
        if (tradingValue >= candidateConfig.getTradingValueThreshold()) {
            return true;
        }
        if (priceChange.abs().compareTo(BigDecimal.valueOf(candidateConfig.getPriceChangeThreshold())) >= 0) {
            return true;
        }
        BigDecimal volumeThreshold = BigDecimal.valueOf(candidateConfig.getVolumeRatioThreshold()).multiply(BigDecimal.valueOf(100));
        if (volumeRatio.compareTo(volumeThreshold) >= 0) {
            return true;
        }
        return newsSignal.newsCount() >= candidateConfig.getNewsBurstThreshold();
    }

    private SelectionWindow resolveSelectionWindow(LocalDate runDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime referenceTime = runDate.isBefore(today)
            ? runDate.plusDays(1).atStartOfDay()
            : LocalDateTime.now(clock);
        return new SelectionWindow(referenceTime.minusHours(candidateConfig.getDisclosureLookbackHours()), referenceTime);
    }

    private Map<String, StockPrice> resolveLatestPrices(List<StockMaster> stocks, LocalDate runDate) {
        Map<String, StockPrice> latestPrices = new HashMap<>();
        for (StockMaster stock : stocks) {
            stockPriceRepository.findTopByTickerAndTradeDateLessThanEqualOrderByTradeDateDesc(stock.getTicker(), runDate)
                .ifPresent(price -> latestPrices.put(stock.getTicker(), price));
        }
        return latestPrices;
    }

    private Map<String, Integer> resolveTradingValueRanks(Map<String, StockPrice> latestPrices) {
        List<StockPrice> ranked = latestPrices.values().stream()
            .sorted(Comparator.comparingLong(this::resolveTradingValue).reversed())
            .toList();

        Map<String, Integer> ranks = new HashMap<>();
        for (int index = 0; index < ranked.size(); index++) {
            ranks.put(ranked.get(index).getTicker(), index + 1);
        }
        return ranks;
    }

    private BigDecimal calculateVolumeRatio(StockPrice latestPrice) {
        if (latestPrice == null || latestPrice.getVolume() == null || latestPrice.getAvgVolume20d() == null || latestPrice.getAvgVolume20d() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(latestPrice.getVolume())
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(latestPrice.getAvgVolume20d()), 2, RoundingMode.HALF_UP);
    }

    private boolean isLargeCap(StockMaster stock) {
        return stock.getMarketCap() != null && stock.getMarketCap() >= candidateConfig.getLargeCapMarketCapThreshold();
    }

    private long resolveTradingValue(StockPrice price) {
        return Optional.ofNullable(price.getTradingValue()).orElse(0L);
    }

    private List<String> parseAliasKeywords(String aliases) {
        if (aliases == null || aliases.isBlank()) {
            return List.of();
        }
        return List.of(aliases.split("[,/|]"))
            .stream()
            .map(this::normalize)
            .filter(keyword -> !keyword.isBlank())
            .distinct()
            .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace(" ", "").trim();
    }

    private String formatWon(long amount) {
        if (amount >= 1_0000_0000_0000L) {
            return String.format(Locale.ROOT, "%.1f조", amount / 1_0000_0000_0000.0);
        }
        if (amount >= 1_0000_0000L) {
            return String.format(Locale.ROOT, "%.1f억", amount / 1_0000_0000.0);
        }
        return String.format(Locale.ROOT, "%,d원", amount);
    }

    private record SelectionWindow(LocalDateTime start, LocalDateTime end) {}

    private record NewsSignal(int newsCount, int themeKeywordHits) {
        private static final NewsSignal EMPTY = new NewsSignal(0, 0);
    }
}

