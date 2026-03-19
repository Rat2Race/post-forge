package dev.iamrat.crawl.service;

import dev.iamrat.crawl.common.AiDocumentSender;
import dev.iamrat.crawl.config.DartConfig;
import dev.iamrat.crawl.dto.DartDisclosureItem;
import dev.iamrat.crawl.dto.DartDisclosureResponse;
import dev.iamrat.crawl.dto.DartFinancialItem;
import dev.iamrat.crawl.dto.DartFinancialResponse;
import dev.iamrat.crawl.entity.CrawledArticle;
import dev.iamrat.crawl.repository.CrawledArticleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
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
        @DisplayName("새 공시가 있으면 AI 전송 후 DB에 저장한다")
        void crawl_newDisclosures_sendsToAiAndSaves() {
            // given
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(item)));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(aiDocumentSender.send(anyList())).willReturn(true);

            // when
            dartCrawlService.crawl();

            // then
            verify(aiDocumentSender, atLeastOnce()).send(anyList());
            verify(crawledArticleRepository, atLeastOnce()).saveAll(anyList());
        }

        @Test
        @DisplayName("AI 전송 실패 시 DB에 저장하지 않는다")
        void crawl_aiSendFails_doesNotSave() {
            // given
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(item)));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(aiDocumentSender.send(anyList())).willReturn(false);

            // when
            dartCrawlService.crawl();

            // then
            verify(crawledArticleRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("이미 저장된 공시는 필터링한다")
        void crawl_duplicateDisclosures_filtered() {
            // given
            DartDisclosureItem item = createItem("주요사항보고서", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(item)));

            CrawledArticle existing = CrawledArticle.builder()
                    .originalLink("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=20260315001234")
                    .build();
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of(existing));

            // when
            dartCrawlService.crawl();

            // then
            verify(aiDocumentSender, never()).send(anyList());
            verify(crawledArticleRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("API 응답이 없으면 처리하지 않는다")
        void crawl_emptyResponse_skips() {
            // given
            stubDisclosureResponse(null);

            // when
            dartCrawlService.crawl();

            // then
            verify(aiDocumentSender, never()).send(anyList());
            verify(crawledArticleRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("종목코드가 없는 공시는 필터링한다")
        void crawl_noStockCode_filtered() {
            // given
            DartDisclosureItem itemNoCode = new DartDisclosureItem(
                    "00126380", "금융감독원", "", "Y", "주요사항보고서",
                    "20260315009999", "제출인", "20260315", null);
            stubDisclosureResponse(okResponse(List.of(itemNoCode)));

            // when
            dartCrawlService.crawl();

            // then
            verify(aiDocumentSender, never()).send(anyList());
        }
    }

    @Nested
    @DisplayName("정기공시 - 재무 수치 크롤링 및 게시글 생성 트리거")
    class PeriodicDisclosureTests {

        @Test
        @DisplayName("정기공시(A) 크롤링 시 재무 수치를 추가 크롤링하고 게시글 생성을 트리거한다")
        void crawl_periodicDisclosure_crawlsFinancialsAndTriggersPost() {
            // given — 재무 수치 API 스텁을 먼저 설정 (deep stub 체인 충돌 방지)
            DartFinancialItem financialItem = new DartFinancialItem(
                    "20260315001234", "2025", "005930", "11011",
                    "매출액", "CFS", "연결재무제표", "IS", "손익계산서",
                    "제 57 기", "78000000000", "제 56 기", "71000000000", "1"
            );
            given(dartRestClient.get().uri(any(java.util.function.Function.class))
                    .retrieve().body(DartFinancialResponse.class))
                    .willReturn(new DartFinancialResponse("000", "정상", List.of(financialItem)));

            // given — 공시 목록 (모든 타입에 정기공시 항목 반환, type A에서 재무+트리거 발생)
            DartDisclosureItem periodicItem = createItem("사업보고서 (2025.12)", "20260315001234");
            stubDisclosureResponse(okResponse(List.of(periodicItem)));
            given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
            given(aiDocumentSender.send(anyList())).willReturn(true);

            // when
            dartCrawlService.crawl();

            // then — 공시 AI 전송 + DB 저장
            verify(aiDocumentSender, atLeastOnce()).send(anyList());
            verify(crawledArticleRepository, atLeastOnce()).saveAll(anyList());
            // then — 게시글 생성 트리거 (type A에서만 발생, stockCode 기준 중복 제거)
            verify(aiDocumentSender).triggerPostGeneration("005930", "삼성전자");
        }
    }
}
