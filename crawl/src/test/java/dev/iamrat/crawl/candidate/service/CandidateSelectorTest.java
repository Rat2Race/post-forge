package dev.iamrat.crawl.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.crawl.candidate.config.CandidateConfig;
import dev.iamrat.crawl.candidate.entity.CandidateSelection;
import dev.iamrat.crawl.candidate.repository.CandidateSelectionRepository;
import dev.iamrat.crawl.common.entity.CrawledArticle;
import dev.iamrat.crawl.common.repository.CrawledArticleRepository;
import dev.iamrat.crawl.price.entity.StockPrice;
import dev.iamrat.crawl.price.repository.StockPriceRepository;
import dev.iamrat.crawl.stock.entity.StockMaster;
import dev.iamrat.crawl.stock.repository.StockMasterRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CandidateSelectorTest {

    @Mock
    private CandidateSelectionRepository candidateSelectionRepository;

    @Mock
    private StockMasterRepository stockMasterRepository;

    @Mock
    private StockPriceRepository stockPriceRepository;

    @Mock
    private CrawledArticleRepository crawledArticleRepository;

    @Mock
    private CandidateConfig candidateConfig;

    @Test
    @DisplayName("거래대금 상위, 급등, 뉴스 집중 신호가 있으면 실전형 후보로 저장한다")
    void select_whenMarketSignalsMatch_savesCandidate() {
        CandidateSelector candidateSelector = createSelector("2026-03-23T00:30:00Z");
        LocalDate runDate = LocalDate.of(2026, 3, 23);
        StockMaster stock = new StockMaster("005930", "삼성전자", null, "KOSPI", "반도체,AI", 12_000_000_000_000L, null, false, false, false, false, null);
        StockPrice price = new StockPrice(
            1L,
            "005930",
            "삼성전자",
            "KOSPI",
            runDate.minusDays(1),
            70000L,
            69000L,
            71000L,
            68000L,
            5_000_000L,
            350_000_000_000L,
            new BigDecimal("4.50"),
            2_000_000L,
            null,
            null,
            LocalDateTime.now()
        );
        CrawledArticle news1 = CrawledArticle.builder().title("삼성전자 AI 반도체 수요 급증").source("naver-news").keyword("삼성전자").originalLink("n1").build();
        CrawledArticle news2 = CrawledArticle.builder().title("AI 테마 강세 속 삼성전자 거래대금 급증").source("naver-news").keyword("삼성전자").originalLink("n2").build();
        CrawledArticle news3 = CrawledArticle.builder().title("반도체 대형주 삼성전자 급등").source("naver-news").keyword("삼성전자").originalLink("n3").build();

        stubConfig();
        given(stockMasterRepository.findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc())
            .willReturn(List.of(stock));
        given(stockPriceRepository.findTopByTickerAndTradeDateLessThanEqualOrderByTradeDateDesc("005930", runDate))
            .willReturn(Optional.of(price));
        given(crawledArticleRepository.findBySourceAndCrawledAtBetween(eq("naver-news"), any(), any()))
            .willReturn(List.of(news1, news2, news3));
        given(crawledArticleRepository.countBySourceAndKeywordAndCrawledAtBetween(any(), any(), any(), any()))
            .willReturn(1L);
        given(candidateSelectionRepository.save(any(CandidateSelection.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        List<CandidateSelection> results = candidateSelector.select(runDate);

        assertThat(results).hasSize(1);
        CandidateSelection selection = results.getFirst();
        assertThat(selection.getReason()).contains("거래대금 상위 1위권");
        assertThat(selection.getReason()).contains("급등 4.50%");
        assertThat(selection.getReason()).contains("뉴스 집중 3건");
        assertThat(selection.getReason()).contains("테마 키워드 반응");
        assertThat(selection.isLargeCap()).isTrue();
        assertThat(selection.getTradingValue()).isEqualTo(350_000_000_000L);
        assertThat(selection.getThemeKeywordHits()).isGreaterThanOrEqualTo(2);
        verify(candidateSelectionRepository).deleteByRunDate(runDate);
    }

    @Test
    @DisplayName("후보 선정 시간창은 요청한 runDate 기준으로 계산해야 한다")
    void select_usesRequestedRunDateForSignalWindow() {
        CandidateSelector candidateSelector = createSelector("2026-03-25T00:30:00Z");
        LocalDate runDate = LocalDate.of(2026, 3, 23);
        StockMaster stock = new StockMaster("005930", "삼성전자", null, "KOSPI", null, 100L, null, false, false, false, false, null);
        StockPrice price = new StockPrice(
            1L,
            "005930",
            "삼성전자",
            "KOSPI",
            runDate,
            70000L,
            69000L,
            71000L,
            68000L,
            5000L,
            350_000_000L,
            new BigDecimal("1.20"),
            2000L,
            null,
            null,
            LocalDateTime.now()
        );

        stubConfig();
        given(stockMasterRepository.findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc())
            .willReturn(List.of(stock));
        given(stockPriceRepository.findTopByTickerAndTradeDateLessThanEqualOrderByTradeDateDesc("005930", runDate))
            .willReturn(Optional.of(price));
        given(crawledArticleRepository.findBySourceAndCrawledAtBetween(eq("naver-news"), any(), any()))
            .willReturn(List.of());
        given(crawledArticleRepository.countBySourceAndKeywordAndCrawledAtBetween(any(), any(), any(), any()))
            .willReturn(0L);

        candidateSelector.select(runDate);

        verify(crawledArticleRepository).countBySourceAndKeywordAndCrawledAtBetween(
            eq("dart"),
            eq("005930"),
            eq(LocalDateTime.of(2026, 3, 23, 6, 0)),
            eq(LocalDateTime.of(2026, 3, 24, 0, 0))
        );
        verify(crawledArticleRepository).findBySourceAndCrawledAtBetween(
            eq("naver-news"),
            eq(LocalDateTime.of(2026, 3, 23, 6, 0)),
            eq(LocalDateTime.of(2026, 3, 24, 0, 0))
        );
    }

    @Test
    @DisplayName("신호가 부족하면 후보로 저장하지 않는다")
    void select_whenSignalsAreWeak_skipsCandidate() {
        CandidateSelector candidateSelector = createSelector("2026-03-23T00:30:00Z");
        LocalDate runDate = LocalDate.of(2026, 3, 23);
        StockMaster stock = new StockMaster("005930", "삼성전자", null, "KOSPI", null, 1_000_000_000L, null, false, false, false, false, null);
        StockPrice price = new StockPrice(
            1L,
            "005930",
            "삼성전자",
            "KOSPI",
            runDate.minusDays(1),
            70000L,
            69000L,
            71000L,
            68000L,
            5000L,
            100_000_000L,
            new BigDecimal("1.20"),
            4900L,
            null,
            null,
            LocalDateTime.now()
        );

        stubConfig();
        given(stockMasterRepository.findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc())
            .willReturn(List.of(stock));
        given(stockPriceRepository.findTopByTickerAndTradeDateLessThanEqualOrderByTradeDateDesc("005930", runDate))
            .willReturn(Optional.of(price));
        given(crawledArticleRepository.findBySourceAndCrawledAtBetween(eq("naver-news"), any(), any()))
            .willReturn(List.of());
        given(crawledArticleRepository.countBySourceAndKeywordAndCrawledAtBetween(any(), any(), any(), any()))
            .willReturn(0L);

        List<CandidateSelection> results = candidateSelector.select(runDate);

        assertThat(results).isEmpty();
    }

    private void stubConfig() {
        given(candidateConfig.getDisclosureLookbackHours()).willReturn(18);
        given(candidateConfig.getPriceChangeThreshold()).willReturn(3.0);
        given(candidateConfig.getVolumeRatioThreshold()).willReturn(2.0);
        given(candidateConfig.getTradingValueThreshold()).willReturn(30_000_000_000L);
        given(candidateConfig.getTopTradingValueRankThreshold()).willReturn(20);
        given(candidateConfig.getNewsBurstThreshold()).willReturn(3);
        given(candidateConfig.getThemeKeywordMinHits()).willReturn(2);
        given(candidateConfig.getLargeCapMarketCapThreshold()).willReturn(10_000_000_000_000L);
    }

    private CandidateSelector createSelector(String utcInstant) {
        Clock clock = Clock.fixed(Instant.parse(utcInstant), ZoneId.of("Asia/Seoul"));
        return new CandidateSelector(
            candidateSelectionRepository,
            stockMasterRepository,
            stockPriceRepository,
            crawledArticleRepository,
            candidateConfig,
            clock
        );
    }
}
