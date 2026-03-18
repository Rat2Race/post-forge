package dev.iamrat.crawl.service;

import dev.iamrat.crawl.common.AiDocumentSender;
import dev.iamrat.crawl.common.DataSourceCrawler;
import dev.iamrat.crawl.config.DartConfig;
import dev.iamrat.crawl.dto.DartDisclosureItem;
import dev.iamrat.crawl.dto.DartDisclosureResponse;
import dev.iamrat.crawl.dto.DocumentRequest;
import dev.iamrat.crawl.entity.CrawledArticle;
import dev.iamrat.crawl.repository.CrawledArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DartCrawlService implements DataSourceCrawler {

    private static final String SOURCE_NAME = "dart";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DART_VIEWER_URL = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";

    /** 주요사항보고(B), 정기공시(A), 지분공시(D) */
    private static final String[] DISCLOSURE_TYPES = {"B", "A", "D"};

    private final RestClient dartRestClient;
    private final DartConfig dartConfig;
    private final CrawledArticleRepository crawledArticleRepository;
    private final AiDocumentSender aiDocumentSender;

    @Override
    public void crawl() {
        String today = LocalDate.now().format(DATE_FMT);
        String weekAgo = LocalDate.now().minusDays(7).format(DATE_FMT);
        int totalNew = 0;

        for (String type : DISCLOSURE_TYPES) {
            int count = crawlByType(type, weekAgo, today);
            totalNew += count;
        }

        log.info("[{}] 크롤링 완료 - 총 {}건의 새 공시 저장", SOURCE_NAME, totalNew);
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    private int crawlByType(String pblntfTy, String beginDate, String endDate) {
        List<DartDisclosureItem> items = fetchDisclosures(pblntfTy, beginDate, endDate);
        if (items.isEmpty()) return 0;

        List<DartDisclosureItem> newItems = filterNewItems(items);
        if (newItems.isEmpty()) return 0;

        if (!aiDocumentSender.send(toDocumentRequests(newItems))) {
            return 0;
        }
        crawledArticleRepository.saveAll(toArticles(newItems));

        log.info("[{}] 공시유형 {} - {}건 새 공시 저장", SOURCE_NAME, pblntfTy, newItems.size());
        return newItems.size();
    }

    private List<DartDisclosureItem> fetchDisclosures(String pblntfTy, String beginDate, String endDate) {
        DartDisclosureResponse response;
        try {
            response = dartRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/list.json")
                            .queryParam("crtfc_key", dartConfig.getApiKey())
                            .queryParam("bgn_de", beginDate)
                            .queryParam("end_de", endDate)
                            .queryParam("pblntf_ty", pblntfTy)
                            .queryParam("corp_cls", "Y")
                            .queryParam("sort", "date")
                            .queryParam("sort_mth", "desc")
                            .queryParam("page_count", dartConfig.getPageCount())
                            .build())
                    .retrieve()
                    .body(DartDisclosureResponse.class);
        } catch (Exception e) {
            log.error("[{}] 공시 조회 실패 (type={})", SOURCE_NAME, pblntfTy, e);
            return Collections.emptyList();
        }

        if (response == null || !"000".equals(response.status()) || response.list() == null) {
            log.debug("[{}] 공시 없음 또는 오류 (type={}, status={})",
                    SOURCE_NAME, pblntfTy, response != null ? response.status() : "null");
            return Collections.emptyList();
        }

        return response.list().stream()
                .filter(item -> item.stockCode() != null && !item.stockCode().isBlank())
                .toList();
    }

    private List<DartDisclosureItem> filterNewItems(List<DartDisclosureItem> items) {
        Set<String> incomingLinks = items.stream()
                .map(item -> DART_VIEWER_URL + item.rceptNo())
                .collect(Collectors.toSet());

        Set<String> existingLinks = crawledArticleRepository.findByOriginalLinkIn(incomingLinks).stream()
                .map(CrawledArticle::getOriginalLink)
                .collect(Collectors.toSet());

        return items.stream()
                .filter(item -> !existingLinks.contains(DART_VIEWER_URL + item.rceptNo()))
                .toList();
    }

    private List<CrawledArticle> toArticles(List<DartDisclosureItem> items) {
        return items.stream()
                .map(item -> CrawledArticle.builder()
                        .originalLink(DART_VIEWER_URL + item.rceptNo())
                        .title("[" + item.corpName() + "] " + item.reportNm())
                        .source(SOURCE_NAME)
                        .keyword(item.stockCode())
                        .publishedAt(item.rceptDt())
                        .build())
                .toList();
    }

    private List<DocumentRequest> toDocumentRequests(List<DartDisclosureItem> items) {
        return items.stream()
                .map(item -> new DocumentRequest(
                        buildContent(item),
                        SOURCE_NAME,
                        Map.of(
                                "corpName", item.corpName(),
                                "stockCode", item.stockCode(),
                                "reportName", item.reportNm(),
                                "originalLink", DART_VIEWER_URL + item.rceptNo(),
                                "publishedAt", item.rceptDt() != null ? item.rceptDt() : ""
                        )
                ))
                .toList();
    }

    private String buildContent(DartDisclosureItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("[DART 공시] ").append(item.corpName());
        sb.append(" (").append(item.stockCode()).append(")\n\n");
        sb.append("보고서: ").append(item.reportNm()).append("\n");
        sb.append("접수일: ").append(item.rceptDt()).append("\n");
        if (item.flrNm() != null && !item.flrNm().isBlank()) {
            sb.append("제출인: ").append(item.flrNm()).append("\n");
        }
        if (item.rm() != null && !item.rm().isBlank()) {
            sb.append("비고: ").append(item.rm());
        }
        return sb.toString().trim();
    }
}
