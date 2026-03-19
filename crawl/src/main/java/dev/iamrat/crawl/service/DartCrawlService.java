package dev.iamrat.crawl.service;

import dev.iamrat.crawl.common.AiDocumentSender;
import dev.iamrat.crawl.common.DataSourceCrawler;
import dev.iamrat.crawl.config.DartConfig;
import dev.iamrat.crawl.dto.DartDisclosureItem;
import dev.iamrat.crawl.dto.DartDisclosureResponse;
import dev.iamrat.crawl.dto.DartFinancialItem;
import dev.iamrat.crawl.dto.DartFinancialResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DartCrawlService implements DataSourceCrawler {

    private static final String SOURCE_NAME = "dart";
    private static final String FINANCIAL_SOURCE_NAME = "dart-financial";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DART_VIEWER_URL = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";
    private static final Pattern REPORT_DATE_PATTERN = Pattern.compile("\\((\\d{4})\\.(\\d{2})\\)");

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
            List<DartDisclosureItem> newItems = crawlByType(type, weekAgo, today);
            totalNew += newItems.size();

            if ("A".equals(type) && !newItems.isEmpty()) {
                crawlFinancials(newItems);
                triggerPostGeneration(newItems);
            }
        }

        log.info("[{}] 크롤링 완료 - 총 {}건의 새 공시 저장", SOURCE_NAME, totalNew);
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    // ── 공시 목록 크롤링 ──

    private List<DartDisclosureItem> crawlByType(String pblntfTy, String beginDate, String endDate) {
        List<DartDisclosureItem> items = fetchDisclosures(pblntfTy, beginDate, endDate);
        if (items.isEmpty()) return Collections.emptyList();

        List<DartDisclosureItem> newItems = filterNewItems(items);
        if (newItems.isEmpty()) return Collections.emptyList();

        if (!aiDocumentSender.send(toDocumentRequests(newItems))) {
            return Collections.emptyList();
        }
        crawledArticleRepository.saveAll(toArticles(newItems));

        log.info("[{}] 공시유형 {} - {}건 새 공시 저장", SOURCE_NAME, pblntfTy, newItems.size());
        return newItems;
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

    // ── 재무 수치 크롤링 (정기공시 감지 시) ──

    private record ReportInfo(String bsnsYear, String reprtCode) {}

    private void crawlFinancials(List<DartDisclosureItem> periodicDisclosures) {
        int count = 0;
        for (DartDisclosureItem item : periodicDisclosures) {
            ReportInfo info = parseReportInfo(item.reportNm());
            if (info == null) continue;

            List<DartFinancialItem> financials = fetchFinancialData(
                    item.corpCode(), info.bsnsYear(), info.reprtCode());
            if (financials.isEmpty()) continue;

            DocumentRequest request = new DocumentRequest(
                    buildFinancialContent(item, financials),
                    FINANCIAL_SOURCE_NAME,
                    Map.of(
                            "corpName", item.corpName(),
                            "stockCode", item.stockCode(),
                            "bsnsYear", info.bsnsYear(),
                            "reprtCode", info.reprtCode(),
                            "originalLink", DART_VIEWER_URL + item.rceptNo(),
                            "publishedAt", item.rceptDt() != null ? item.rceptDt() : ""
                    )
            );

            if (aiDocumentSender.send(List.of(request))) {
                count++;
            }
        }
        if (count > 0) {
            log.info("[{}] {}건의 재무 수치 저장", FINANCIAL_SOURCE_NAME, count);
        }
    }

    private ReportInfo parseReportInfo(String reportNm) {
        if (reportNm == null) return null;

        Matcher matcher = REPORT_DATE_PATTERN.matcher(reportNm);
        if (!matcher.find()) return null;

        String year = matcher.group(1);
        String month = matcher.group(2);

        String reprtCode;
        if (reportNm.contains("사업보고서")) {
            reprtCode = "11011";
        } else if (reportNm.contains("반기")) {
            reprtCode = "11012";
        } else if (reportNm.contains("분기")) {
            int m = Integer.parseInt(month);
            reprtCode = (m <= 6) ? "11013" : "11014";
        } else {
            return null;
        }

        return new ReportInfo(year, reprtCode);
    }

    private List<DartFinancialItem> fetchFinancialData(String corpCode, String bsnsYear, String reprtCode) {
        DartFinancialResponse response;
        try {
            response = dartRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/fnlttSinglAcnt.json")
                            .queryParam("crtfc_key", dartConfig.getApiKey())
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", reprtCode)
                            .build())
                    .retrieve()
                    .body(DartFinancialResponse.class);
        } catch (Exception e) {
            log.error("[{}] 재무 수치 조회 실패 (corpCode={})", FINANCIAL_SOURCE_NAME, corpCode, e);
            return Collections.emptyList();
        }

        if (response == null || !"000".equals(response.status()) || response.list() == null) {
            return Collections.emptyList();
        }

        return filterConsolidated(response.list());
    }

    private List<DartFinancialItem> filterConsolidated(List<DartFinancialItem> items) {
        boolean hasCfs = items.stream().anyMatch(i -> "CFS".equals(i.fsDiv()));
        String targetFsDiv = hasCfs ? "CFS" : "OFS";
        return items.stream()
                .filter(i -> targetFsDiv.equals(i.fsDiv()))
                .toList();
    }

    private String buildFinancialContent(DartDisclosureItem disclosure, List<DartFinancialItem> financials) {
        StringBuilder sb = new StringBuilder();
        sb.append("## DART 재무 수치 — ").append(disclosure.corpName())
                .append(" (").append(disclosure.stockCode()).append(")\n\n");
        sb.append("- 보고서: ").append(disclosure.reportNm()).append("\n");
        sb.append("- 접수일: ").append(disclosure.rceptDt()).append("\n\n");
        sb.append("| 항목 | 당기 | 전기 | 증감률 |\n");
        sb.append("|------|------|------|--------|\n");

        for (DartFinancialItem item : financials) {
            String changeRate = calculateChangeRate(item.thstrmAmount(), item.frmtrmAmount());
            sb.append("| ").append(item.accountNm())
                    .append(" | ").append(formatAmount(item.thstrmAmount()))
                    .append(" | ").append(formatAmount(item.frmtrmAmount()))
                    .append(" | ").append(changeRate)
                    .append(" |\n");
        }

        return sb.toString().trim();
    }

    private String calculateChangeRate(String currentStr, String previousStr) {
        try {
            long current = parseAmount(currentStr);
            long previous = parseAmount(previousStr);
            if (previous == 0) return "-";
            double rate = ((double) (current - previous) / Math.abs(previous)) * 100;
            return String.format("%+.1f%%", rate);
        } catch (Exception e) {
            return "-";
        }
    }

    private long parseAmount(String amount) {
        if (amount == null || amount.isBlank() || "-".equals(amount.trim())) return 0;
        return Long.parseLong(amount.replaceAll("[^\\d-]", ""));
    }

    private String formatAmount(String amount) {
        if (amount == null || amount.isBlank()) return "-";
        try {
            long value = Long.parseLong(amount.replaceAll("[^\\d-]", ""));
            return String.format("%,d", value);
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    // ── 게시글 자동 생성 트리거 ──

    private void triggerPostGeneration(List<DartDisclosureItem> items) {
        items.stream()
                .collect(Collectors.toMap(
                        DartDisclosureItem::stockCode,
                        item -> item,
                        (a, b) -> a
                ))
                .values()
                .forEach(item -> aiDocumentSender.triggerPostGeneration(
                        item.stockCode(), item.corpName()));
    }
}
