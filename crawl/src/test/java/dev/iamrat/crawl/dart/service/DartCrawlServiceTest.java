package dev.iamrat.crawl.dart.service;

import dev.iamrat.crawl.common.AiDocumentSender;
import dev.iamrat.crawl.common.dto.DocumentRequest;
import dev.iamrat.crawl.common.entity.CrawledArticle;
import dev.iamrat.crawl.common.repository.CrawledArticleRepository;
import dev.iamrat.crawl.dart.config.DartConfig;
import dev.iamrat.crawl.dart.dto.DartDisclosureItem;
import dev.iamrat.crawl.dart.dto.DartDisclosureResponse;
import dev.iamrat.crawl.dart.dto.DartFinancialItem;
import dev.iamrat.crawl.dart.dto.DartFinancialResponse;
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

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
    private AiDocumentSender aiDocumentSender;

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

    @Nested
    @DisplayName("공시 크롤링")
    class CrawlTests {

        @Test
        @DisplayName("새 공시가 있으면 DB에 먼저 저장하고 메인 앱으로 전송한다")
        void crawl_newDisclosures_savesThenSends() {
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(item)));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(aiDocumentSender.send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList())).willReturn(true);

            dartCrawlService.crawl();

            InOrder inOrder = inOrder(crawledArticleRepository, aiDocumentSender);
            inOrder.verify(crawledArticleRepository, atLeastOnce()).saveAll(anyList());
            inOrder.verify(aiDocumentSender, atLeastOnce()).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
        }

        @Test
        @DisplayName("메인 앱 전송 실패 시에도 DB 저장은 유지한다")
        void crawl_sendFails_keepsSavedDisclosures() {
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(item)));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(aiDocumentSender.send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList())).willReturn(false);

            dartCrawlService.crawl();

            verify(crawledArticleRepository, atLeastOnce()).saveAll(anyList());
            verify(aiDocumentSender, atLeastOnce()).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
        }

        @Test
        @DisplayName("이미 저장된 공시는 필터링한다")
        void crawl_duplicateDisclosures_filtered() {
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(item)));

            CrawledArticle existing = mock(CrawledArticle.class);
            given(existing.getOriginalLink()).willReturn("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=20260315001234");
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of(existing));

            dartCrawlService.crawl();

            verify(aiDocumentSender, never()).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
            verify(crawledArticleRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("API 응답이 없으면 처리하지 않는다")
        void crawl_emptyResponse_skips() {
            stubDisclosureResponse(null);

            dartCrawlService.crawl();

            verify(aiDocumentSender, never()).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
            verify(crawledArticleRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("종목코드가 없는 공시는 필터링한다")
        void crawl_noStockCode_filtered() {
            DartDisclosureItem itemNoCode = new DartDisclosureItem(
                    "00126380", "금융감독원", "", "Y", "주요사항보고서",
                    "20260315009999", "제출인", "20260315", null);
            stubDisclosureResponse(okResponse(List.of(itemNoCode)));

            dartCrawlService.crawl();

            verify(aiDocumentSender, never()).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
        }
    }

    @Nested
    @DisplayName("정기공시 - 재무 수치 크롤링")
    class PeriodicDisclosureTests {

        @Test
        @DisplayName("정기공시(A) 크롤링 시 재무 수치를 추가 전송하지만 crawl에서 직접 게시글 생성은 하지 않는다")
        void crawl_periodicDisclosure_crawlsFinancialsWithoutDirectPostTrigger() {
            DartFinancialItem financialItem = new DartFinancialItem(
                    "20260315001234", "2025", "005930", "11011",
                    "매출액", "CFS", "연결재무제표", "IS", "손익계산서",
                    "제 57 기", "78000000000", "제 56 기", "71000000000", "1"
            );
            given(dartRestClient.get().uri(any(java.util.function.Function.class))
                    .retrieve().body(DartFinancialResponse.class))
                    .willReturn(new DartFinancialResponse("000", "정상", List.of(financialItem)));

            DartDisclosureItem periodicItem = createItem("사업보고서 (2025.12)", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(periodicItem)));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(aiDocumentSender.send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList())).willReturn(true);

            dartCrawlService.crawl();

            verify(aiDocumentSender, atLeast(2)).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
            verify(crawledArticleRepository, atLeastOnce()).saveAll(anyList());
            verify(aiDocumentSender, never()).triggerPostGeneration(anyString(), anyString());
        }
    }
}
