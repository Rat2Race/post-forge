package dev.iamrat.crawl.dart.service;

import dev.iamrat.crawl.common.AiDocumentSender;
import dev.iamrat.crawl.common.DataSourceCrawler;
import dev.iamrat.crawl.common.dto.DocumentRequest;
import dev.iamrat.crawl.common.entity.CrawledArticle;
import dev.iamrat.crawl.common.repository.CrawledArticleRepository;
import dev.iamrat.crawl.dart.config.DartConfig;
import dev.iamrat.crawl.dart.dto.DartDisclosureItem;
import dev.iamrat.crawl.dart.dto.DartDisclosureResponse;
import dev.iamrat.crawl.dart.dto.DartFinancialItem;
import dev.iamrat.crawl.dart.dto.DartFinancialResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class DartCrawlService implements DataSourceCrawler {

    private static final String DART_API_SUCCESS_STATUS = "000";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DART_VIEWER_URL = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";
    private static final Pattern REPORT_DATE_PATTERN = Pattern.compile("\\((\\d{4})\\.(\\d{2})\\)");
    private static final List<String> DISCLOSURE_TYPES = List.of(
            DisclosureTypes.MAJOR,
            DisclosureTypes.PERIODIC,
            DisclosureTypes.EQUITY
    );

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

            if (DisclosureTypes.PERIODIC.equals(type) && !newItems.isEmpty()) {
                crawlFinancials(newItems);
            }
        }

        log.info("[{}] 크롤링 완료 - 총 {}건의 새 공시 저장", SourceNames.DART, totalNew);
    }

    @Override
    public String getSourceName() {
        return SourceNames.DART;
    }

    private List<DartDisclosureItem> crawlByType(String pblntfTy, String beginDate, String endDate) {
        List<DartDisclosureItem> items = fetchDisclosures(pblntfTy, beginDate, endDate);
        if (items.isEmpty()) return Collections.emptyList();

        List<DartDisclosureItem> newItems = filterNewItems(items);
        if (newItems.isEmpty()) return Collections.emptyList();

        crawledArticleRepository.saveAll(toArticles(newItems));
        if (!aiDocumentSender.send(toDocumentRequests(newItems))) {
            log.warn("[{}] 공시유형 {} - 메인 앱 전송 실패, H2 저장 상태만 유지", SourceNames.DART, pblntfTy);
        }

        log.info("[{}] 공시유형 {} - {}건 새 공시 저장", SourceNames.DART, pblntfTy, newItems.size());
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
            log.error("[{}] 공시 조회 실패 (type={})", SourceNames.DART, pblntfTy, e);
            return Collections.emptyList();
        }

        if (response == null || !DART_API_SUCCESS_STATUS.equals(response.status()) || response.list() == null) {
            log.debug("[{}] 공시 없음 또는 오류 (type={}, status={})",
                    SourceNames.DART, pblntfTy, response != null ? response.status() : "null");
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
                        .source(SourceNames.DART)
                        .keyword(item.stockCode())
                        .publishedAt(item.rceptDt())
                        .build())
                .toList();
    }

    private List<DocumentRequest> toDocumentRequests(List<DartDisclosureItem> items) {
        return items.stream()
                .map(item -> new DocumentRequest(
                        buildContent(item),
                        SourceNames.DART,
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
        return joinLines(
                "[DART 공시] %s (%s)".formatted(item.corpName(), item.stockCode()),
                "",
                "보고서: %s".formatted(item.reportNm()),
                "접수일: %s".formatted(item.rceptDt()),
                optionalLine("제출인", item.flrNm()),
                optionalLine("비고", item.rm())
        );
    }

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
                    SourceNames.DART_FINANCIAL,
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
            } else {
                log.warn("[{}] {} ({}) 재무 수치 전송 실패", SourceNames.DART_FINANCIAL, item.corpName(), item.stockCode());
            }
        }
        if (count > 0) {
            log.info("[{}] {}건의 재무 수치 저장", SourceNames.DART_FINANCIAL, count);
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
            reprtCode = ReportCodes.ANNUAL;
        } else if (reportNm.contains("반기")) {
            reprtCode = ReportCodes.SEMI_ANNUAL;
        } else if (reportNm.contains("분기")) {
            int m = Integer.parseInt(month);
            reprtCode = (m <= 6) ? ReportCodes.Q1 : ReportCodes.Q3;
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
            log.error("[{}] 재무 수치 조회 실패 (corpCode={})", SourceNames.DART_FINANCIAL, corpCode, e);
            return Collections.emptyList();
        }

        if (response == null || !DART_API_SUCCESS_STATUS.equals(response.status()) || response.list() == null) {
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
        String rows = financials.stream()
                .map(item -> "| %s | %s | %s | %s |".formatted(
                        item.accountNm(),
                        formatAmount(item.thstrmAmount()),
                        formatAmount(item.frmtrmAmount()),
                        calculateChangeRate(item.thstrmAmount(), item.frmtrmAmount())
                ))
                .collect(Collectors.joining("\n"));

        return """
                ## DART 재무 수치 — %s (%s)

                - 보고서: %s
                - 접수일: %s

                | 항목 | 당기 | 전기 | 증감률 |
                |------|------|------|--------|
                %s
                """.formatted(
                disclosure.corpName(),
                disclosure.stockCode(),
                disclosure.reportNm(),
                disclosure.rceptDt(),
                rows
        ).trim();
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

    private String optionalLine(String label, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%s: %s".formatted(label, value);
    }

    private String joinLines(String... lines) {
        return java.util.Arrays.stream(lines)
                .filter(line -> line != null)
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private static final class SourceNames {
        private static final String DART = "dart";
        private static final String DART_FINANCIAL = "dart-financial";

        private SourceNames() {
        }
    }

    private static final class DisclosureTypes {
        private static final String MAJOR = "B";
        private static final String PERIODIC = "A";
        private static final String EQUITY = "D";

        private DisclosureTypes() {
        }
    }

    private static final class ReportCodes {
        private static final String ANNUAL = "11011";
        private static final String SEMI_ANNUAL = "11012";
        private static final String Q1 = "11013";
        private static final String Q3 = "11014";

        private ReportCodes() {
        }
    }
}
