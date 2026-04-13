package dev.iamrat.price.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.common.InternalCrawlClient;
import dev.iamrat.common.DataSourceCrawler;
import dev.iamrat.common.dto.InternalDocumentPayload;
import dev.iamrat.price.config.KrxConfig;
import dev.iamrat.price.dto.KrxDailyPriceSnapshot;
import dev.iamrat.price.entity.StockPrice;
import dev.iamrat.price.repository.StockPriceRepository;
import dev.iamrat.stock.entity.StockMaster;
import dev.iamrat.stock.repository.StockMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KrxPriceCrawlService implements DataSourceCrawler {

    private static final int MAX_TRADE_DATE_LOOKBACK_DAYS = 7;
    private static final String SOURCE_NAME = "krx-price";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient krxRestClient;
    private final KrxConfig krxConfig;
    private final ObjectMapper objectMapper;
    private final StockMasterRepository stockMasterRepository;
    private final StockPriceRepository stockPriceRepository;
    private final InternalCrawlClient internalCrawlClient;
    private final Clock clock;

    public KrxPriceCrawlService(
        RestClient krxRestClient,
        KrxConfig krxConfig,
        ObjectMapper objectMapper,
        StockMasterRepository stockMasterRepository,
        StockPriceRepository stockPriceRepository,
        InternalCrawlClient internalCrawlClient,
        Clock clock
    ) {
        this.krxRestClient = krxRestClient;
        this.krxConfig = krxConfig;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.stockMasterRepository = stockMasterRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.internalCrawlClient = internalCrawlClient;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @Override
    public void crawl() {
        if (!krxConfig.isEnabled()) {
            return;
        }
        if (krxConfig.getApiKey() == null || krxConfig.getApiKey().isBlank()) {
            throw new IllegalStateException("KRX_API_KEY is required for krx-price crawl");
        }

        List<StockMaster> eligibleStocks = stockMasterRepository
            .findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc();
        Set<String> allowedTickers = eligibleStocks.stream().map(StockMaster::getTicker).collect(Collectors.toSet());
        Map<String, StockMaster> stockMap = eligibleStocks.stream()
            .collect(Collectors.toMap(StockMaster::getTicker, stock -> stock));

        List<KrxDailyPriceSnapshot> snapshots = fetchLatestAvailableSnapshots(allowedTickers);

        List<StockPrice> persisted = new ArrayList<>();
        for (KrxDailyPriceSnapshot snapshot : snapshots) {
            Long avgVolume20d = calculateAverageVolume(snapshot.ticker(), snapshot.tradeDate());
            StockPrice price = stockPriceRepository.findByTickerAndTradeDate(snapshot.ticker(), snapshot.tradeDate())
                .map(existing -> updateExistingPrice(existing, snapshot, avgVolume20d))
                .orElseGet(() -> new StockPrice(
                    null,
                    snapshot.ticker(),
                    snapshot.stockName(),
                    snapshot.market(),
                    snapshot.tradeDate(),
                    snapshot.closePrice(),
                    snapshot.openPrice(),
                    snapshot.highPrice(),
                    snapshot.lowPrice(),
                    snapshot.volume(),
                    snapshot.tradingValue(),
                    snapshot.changeRate(),
                    avgVolume20d,
                    null,
                    null,
                    null
                ));

            persisted.add(stockPriceRepository.save(price));
        }

        if (!persisted.isEmpty() && !internalCrawlClient.sendDocuments(toDocumentRequests(persisted, stockMap))) {
            throw new IllegalStateException("메인 앱에 KRX 가격 문서를 전송하지 못했습니다.");
        }
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    public List<StockPrice> getLatestPricesForEligibleStocks() {
        List<StockPrice> results = new ArrayList<>();
        for (StockMaster stock : stockMasterRepository.findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc()) {
            stockPriceRepository.findTopByTickerAndTradeDateLessThanEqualOrderByTradeDateDesc(stock.getTicker(), LocalDate.now(clock))
                .ifPresent(results::add);
        }
        return results;
    }

    private List<KrxDailyPriceSnapshot> fetchLatestAvailableSnapshots(Set<String> allowedTickers) {
        LocalDate today = LocalDate.now(clock);
        for (int dayOffset = 0; dayOffset < MAX_TRADE_DATE_LOOKBACK_DAYS; dayOffset++) {
            LocalDate tradeDate = today.minusDays(dayOffset);
            if (tradeDate.getDayOfWeek() == DayOfWeek.SATURDAY || tradeDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }

            List<KrxDailyPriceSnapshot> snapshots = new ArrayList<>();
            snapshots.addAll(fetchDailyPrices(krxConfig.getKospiEndpoint(), "KOSPI", tradeDate, allowedTickers));
            snapshots.addAll(fetchDailyPrices(krxConfig.getKosdaqEndpoint(), "KOSDAQ", tradeDate, allowedTickers));
            if (!snapshots.isEmpty()) {
                return snapshots;
            }
        }
        throw new IllegalStateException("No KRX daily price data available in the last " + MAX_TRADE_DATE_LOOKBACK_DAYS + " days");
    }

    private List<KrxDailyPriceSnapshot> fetchDailyPrices(String endpoint, String market, LocalDate tradeDate, Set<String> allowedTickers) {
        try {
            String body = krxRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host(extractHost(endpoint))
                    .path(extractPath(endpoint))
                    .queryParam("basDd", tradeDate.format(DATE_FMT))
                    .build())
                .retrieve()
                .body(String.class);

            return parseSnapshots(body, market, tradeDate, allowedTickers);
        } catch (Exception e) {
            throw new IllegalStateException("KRX daily price fetch failed for " + market, e);
        }
    }

    private List<KrxDailyPriceSnapshot> parseSnapshots(String body, String market, LocalDate tradeDate, Set<String> allowedTickers) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode rows = resolveRows(root);
        if (rows == null || !rows.isArray()) {
            return List.of();
        }

        List<KrxDailyPriceSnapshot> snapshots = new ArrayList<>();
        for (JsonNode row : rows) {
            String ticker = getText(row, "ISU_SRT_CD", "mksc_shrn_iscd", "ISU_CD");
            if (ticker == null || ticker.isBlank() || (!allowedTickers.isEmpty() && !allowedTickers.contains(ticker))) {
                continue;
            }

            Long closePrice = parseLong(getText(row, "TDD_CLSPRC"));
            Long volume = parseLong(getText(row, "ACC_TRDVOL"));
            Long tradingValue = resolveTradingValue(row, closePrice, volume);

            snapshots.add(new KrxDailyPriceSnapshot(
                ticker,
                getText(row, "ISU_NM", "ISU_ABBRV", "hts_kor_isnm"),
                market,
                tradeDate,
                closePrice,
                parseLong(getText(row, "TDD_OPNPRC")),
                parseLong(getText(row, "TDD_HGPRC")),
                parseLong(getText(row, "TDD_LWPRC")),
                volume,
                tradingValue,
                parseDecimal(getText(row, "FLUC_RT", "CMPPREVDD_PRC"))
            ));
        }
        return snapshots;
    }

    private List<InternalDocumentPayload> toDocumentRequests(List<StockPrice> prices, Map<String, StockMaster> stockMap) {
        List<InternalDocumentPayload> requests = new ArrayList<>();
        for (StockPrice price : prices) {
            BigDecimal volumeRatio = calculateVolumeRatio(price);
            StockMaster stock = stockMap.get(price.getTicker());
            Map<String, String> metadata = new HashMap<>();
            metadata.put("stockCode", price.getTicker());
            metadata.put("corpName", String.valueOf(price.getStockName()));
            metadata.put("market", String.valueOf(price.getMarket()));
            metadata.put("tradeDate", String.valueOf(price.getTradeDate()));
            metadata.put("changeRate", price.getChangeRate() != null ? price.getChangeRate().toPlainString() : "0");
            metadata.put("volumeRatio", volumeRatio.toPlainString());
            metadata.put("tradingValue", String.valueOf(defaultLong(price.getTradingValue())));
            if (stock != null && stock.getDartCorpCode() != null) {
                metadata.put("dartCorpCode", stock.getDartCorpCode());
            }
            requests.add(new InternalDocumentPayload(buildPriceContent(price, volumeRatio), SOURCE_NAME, metadata));
        }
        return requests;
    }

    private String buildPriceContent(StockPrice price, BigDecimal volumeRatio) {
        return """
            [가격/거래량 데이터] %s (%s)

            거래일: %s
            종가: %,d원
            시가: %,d원
            고가: %,d원
            저가: %,d원
            등락률: %s%%
            거래량: %,d주
            거래대금(추정 포함): %,d원
            20일 평균 거래량: %,d주
            거래량 비율: %s%%
            """.formatted(
            Optional.ofNullable(price.getStockName()).orElse(price.getTicker()),
            price.getTicker(),
            price.getTradeDate(),
            defaultLong(price.getClosePrice()),
            defaultLong(price.getOpenPrice()),
            defaultLong(price.getHighPrice()),
            defaultLong(price.getLowPrice()),
            price.getChangeRate() != null ? price.getChangeRate().toPlainString() : "0",
            defaultLong(price.getVolume()),
            defaultLong(price.getTradingValue()),
            defaultLong(price.getAvgVolume20d()),
            volumeRatio.toPlainString()
        ).trim();
    }

    private Long calculateAverageVolume(String ticker, LocalDate tradeDate) {
        List<StockPrice> recent = stockPriceRepository.findTop20ByTickerAndTradeDateBeforeOrderByTradeDateDesc(ticker, tradeDate);
        List<Long> volumes = recent.stream()
            .map(StockPrice::getVolume)
            .filter(v -> v != null && v > 0)
            .toList();
        if (volumes.isEmpty()) {
            return 0L;
        }
        long total = volumes.stream()
            .mapToLong(Long::longValue)
            .sum();
        return total / volumes.size();
    }

    private StockPrice updateExistingPrice(StockPrice existing, KrxDailyPriceSnapshot snapshot, Long avgVolume20d) {
        existing.applySnapshot(
            snapshot.stockName(),
            snapshot.market(),
            snapshot.closePrice(),
            snapshot.openPrice(),
            snapshot.highPrice(),
            snapshot.lowPrice(),
            snapshot.volume(),
            snapshot.tradingValue(),
            snapshot.changeRate(),
            avgVolume20d
        );
        return existing;
    }

    private BigDecimal calculateVolumeRatio(StockPrice price) {
        if (price.getVolume() == null || price.getAvgVolume20d() == null || price.getAvgVolume20d() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(price.getVolume())
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(price.getAvgVolume20d()), 2, RoundingMode.HALF_UP);
    }

    private Long resolveTradingValue(JsonNode row, Long closePrice, Long volume) {
        Long apiTradingValue = parseLong(getText(row, "ACC_TRDVAL", "acc_trdval"));
        if (apiTradingValue != null && apiTradingValue > 0) {
            return apiTradingValue;
        }
        return defaultLong(closePrice) * defaultLong(volume);
    }

    private JsonNode resolveRows(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("OutBlock_1")) {
            return root.get("OutBlock_1");
        }
        if (root.has("output")) {
            return root.get("output");
        }
        if (root.has("result")) {
            return root.get("result");
        }
        return null;
    }

    private String getText(JsonNode node, String... fields) {
        for (String field : fields) {
            if (node.has(field) && !node.get(field).isNull()) {
                return node.get(field).asText();
            }
        }
        return null;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        String sanitized = value.replaceAll("[^\\d-]", "");
        if (sanitized.isBlank()) {
            return 0L;
        }
        return Long.parseLong(sanitized);
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        String sanitized = value.replaceAll("[^\\d.-]", "");
        if (sanitized.isBlank() || "-".equals(sanitized) || ".".equals(sanitized)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(sanitized);
    }

    private long defaultLong(Long value) {
        return value != null ? value : 0L;
    }

    private String extractHost(String endpoint) {
        return endpoint.replaceFirst("https?://", "").replaceAll("/.*$", "");
    }

    private String extractPath(String endpoint) {
        String withoutScheme = endpoint.replaceFirst("https?://", "");
        int slash = withoutScheme.indexOf('/');
        return slash >= 0 ? withoutScheme.substring(slash) : "";
    }

}

