package dev.iamrat.dart.service;

import dev.iamrat.common.InternalCrawlClient;
import dev.iamrat.common.dto.InternalDocumentPayload;
import dev.iamrat.common.entity.CrawledArticle;
import dev.iamrat.common.repository.CrawledArticleRepository;
import dev.iamrat.dart.config.DartConfig;
import dev.iamrat.dart.dto.DartDisclosureItem;
import dev.iamrat.dart.dto.DartDisclosureResponse;
import dev.iamrat.dart.dto.DartFinancialItem;
import dev.iamrat.dart.dto.DartFinancialResponse;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DartCrawlServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient dartRestClient;

    @Mock
    private DartConfig dartConfig;

    @Mock
    private CrawledArticleRepository crawledArticleRepository;

    @Mock
    private InternalCrawlClient internalCrawlClient;

    @InjectMocks
    private DartCrawlService dartCrawlService;

    private DartDisclosureItem createItem(String reportNm, String rceptNo) {
        return new DartDisclosureItem(
                "00126380", "삼성전자", "005930", "Y", reportNm,
                rceptNo, "제출인", "20260315", null
        );
    }

    private DartDisclosureResponse okResponse(List<DartDisclosureItem> items) {
        return new DartDisclosureResponse("000", "정상", 1, 100, items.size(), 1, items);
    }

    private void stubDisclosureResponse(DartDisclosureResponse response) {
        given(dartRestClient.get().uri(any(java.util.function.Function.class))
                .retrieve().body(DartDisclosureResponse.class)).willReturn(response);
    }

    @Test
    @DisplayName("API 키가 없으면 DART 크롤링을 건너뛴다")
    void crawl_missingApiKey_skips() {
        given(dartConfig.getApiKey()).willReturn("");

        dartCrawlService.crawl();

        verify(dartRestClient, never()).get();
        verify(internalCrawlClient, never()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }

    @Nested
    @DisplayName("공시 크롤링")
    class CrawlTests {

        @Test
        @DisplayName("새 공시가 있으면 DB에 먼저 저장하고 메인 앱으로 전송한다")
        void crawl_newDisclosures_savesThenSends() {
            given(dartConfig.getApiKey()).willReturn("dart-key");
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            given(dartRestClient.get().uri(any(java.util.function.Function.class))
                    .retrieve().body(DartDisclosureResponse.class))
                    .willReturn(okResponse(List.of(item)), okResponse(List.of()), okResponse(List.of()));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(internalCrawlClient.sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList())).willReturn(true);

            dartCrawlService.crawl();

            InOrder inOrder = inOrder(crawledArticleRepository, internalCrawlClient);
            inOrder.verify(crawledArticleRepository, atLeastOnce()).saveAll(anyList());
            inOrder.verify(internalCrawlClient, atLeastOnce()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
            verify(internalCrawlClient, never()).requestPostGeneration(anyString(), anyString());
        }

        @Test
        @DisplayName("메인 앱 전송 실패 시에도 DB 저장은 유지한다")
        void crawl_sendFails_keepsSavedDisclosures() {
            given(dartConfig.getApiKey()).willReturn("dart-key");
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(item)));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(internalCrawlClient.sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList())).willReturn(false);

            dartCrawlService.crawl();

            verify(crawledArticleRepository, atLeastOnce()).saveAll(anyList());
            verify(internalCrawlClient, atLeastOnce()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
        }

        @Test
        @DisplayName("이미 저장된 공시는 필터링한다")
        void crawl_duplicateDisclosures_filtered() {
            given(dartConfig.getApiKey()).willReturn("dart-key");
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(item)));

            CrawledArticle existing = mock(CrawledArticle.class);
            given(existing.getOriginalLink()).willReturn("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=20260315001234");
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of(existing));

            dartCrawlService.crawl();

            verify(internalCrawlClient, never()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
            verify(crawledArticleRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("API 응답이 없으면 처리하지 않는다")
        void crawl_emptyResponse_skips() {
            given(dartConfig.getApiKey()).willReturn("dart-key");
            stubDisclosureResponse(null);

            dartCrawlService.crawl();

            verify(internalCrawlClient, never()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
            verify(crawledArticleRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("종목코드가 없는 공시는 필터링한다")
        void crawl_noStockCode_filtered() {
            given(dartConfig.getApiKey()).willReturn("dart-key");
            DartDisclosureItem itemNoCode = new DartDisclosureItem(
                    "00126380", "금융감독원", "", "Y", "주요사항보고서",
                    "20260315009999", "제출인", "20260315", null);
            stubDisclosureResponse(okResponse(List.of(itemNoCode)));

            dartCrawlService.crawl();

            verify(internalCrawlClient, never()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
        }
    }

    @Nested
    @DisplayName("정기공시 - 재무 수치 크롤링")
    class PeriodicDisclosureTests {

        @Test
        @DisplayName("정기공시(A) 크롤링 시 공시 문서 전송 후 main app에 게시글 생성을 요청한다")
        void crawl_periodicDisclosure_requestsPostGeneration() {
            given(dartConfig.getApiKey()).willReturn("dart-key");
            DartDisclosureItem periodicItem = createItem("사업보고서 (2025.12)", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(periodicItem)));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(internalCrawlClient.sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList())).willReturn(true);

            dartCrawlService.crawl();

            verify(internalCrawlClient, atLeastOnce()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
            verify(crawledArticleRepository, atLeastOnce()).saveAll(anyList());
            verify(internalCrawlClient).requestPostGeneration("005930", "삼성전자");
        }
    }
}

