package dev.iamrat.crawl.news.service;

import dev.iamrat.crawl.common.AiDocumentSender;
import dev.iamrat.crawl.common.dto.DocumentRequest;
import dev.iamrat.crawl.common.entity.CrawledArticle;
import dev.iamrat.crawl.common.repository.CrawledArticleRepository;
import dev.iamrat.crawl.news.config.NaverNewsConfig;
import dev.iamrat.crawl.news.dto.NaverNewsApiResponse;
import dev.iamrat.crawl.news.dto.NaverNewsItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NaverNewsCrawlServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient naverNewsRestClient;

    @Mock
    private NaverNewsConfig naverNewsConfig;

    @Mock
    private CrawledArticleRepository crawledArticleRepository;

    @Mock
    private AiDocumentSender aiDocumentSender;

    @InjectMocks
    private NaverNewsCrawlService naverNewsCrawlService;

    private NaverNewsItem createNewsItem(String title, String link, String originalLink) {
        return new NaverNewsItem(title, originalLink, link, "뉴스 내용 요약",
                "Mon, 15 Mar 2026 09:00:00 +0900");
    }

    @Test
    @DisplayName("새 뉴스가 있으면 DB에 먼저 저장하고 메인 앱으로 전송한다")
    void crawl_newNews_savesThenSends() {
        given(naverNewsConfig.getKeywords()).willReturn("주식");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item = createNewsItem(
                "삼성전자 주가 상승", "https://link", "https://original-link");
        NaverNewsApiResponse response = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item));

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class)).willReturn(response);
        given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
        given(aiDocumentSender.send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList())).willReturn(true);

        naverNewsCrawlService.crawl();

        InOrder inOrder = inOrder(crawledArticleRepository, aiDocumentSender);
        inOrder.verify(crawledArticleRepository).saveAll(anyList());
        inOrder.verify(aiDocumentSender).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
    }

    @Test
    @DisplayName("메인 앱 전송 실패 시에도 H2 저장은 유지한다")
    void crawl_sendFails_keepsSavedArticles() {
        given(naverNewsConfig.getKeywords()).willReturn("주식");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item = createNewsItem(
                "삼성전자 주가 상승", "https://link", "https://original-link");
        NaverNewsApiResponse response = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item));

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class)).willReturn(response);
        given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
        given(aiDocumentSender.send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList())).willReturn(false);

        naverNewsCrawlService.crawl();

        verify(crawledArticleRepository).saveAll(anyList());
        verify(aiDocumentSender).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
    }

    @Test
    @DisplayName("이미 저장된 기사는 필터링한다")
    void crawl_duplicateArticles_filtered() {
        given(naverNewsConfig.getKeywords()).willReturn("주식");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item = createNewsItem(
                "삼성전자 주가 상승", "https://link", "https://original-link");
        NaverNewsApiResponse response = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item));

        CrawledArticle existing = mock(CrawledArticle.class);
        given(existing.getOriginalLink()).willReturn("https://original-link");

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class)).willReturn(response);
        given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of(existing));

        naverNewsCrawlService.crawl();

        verify(aiDocumentSender, never()).send(anyList());
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("여러 키워드를 순회하며 크롤링한다")
    void crawl_multipleKeywords_crawlsAll() {
        given(naverNewsConfig.getKeywords()).willReturn("주식,코스피");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item1 = createNewsItem(
                "주식 뉴스", "https://link1", "https://original1");
        NaverNewsItem item2 = createNewsItem(
                "코스피 뉴스", "https://link2", "https://original2");

        NaverNewsApiResponse response1 = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item1));
        NaverNewsApiResponse response2 = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item2));

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class))
                .willReturn(response1)
                .willReturn(response2);
        given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
        given(aiDocumentSender.send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList())).willReturn(true);

        naverNewsCrawlService.crawl();

        verify(aiDocumentSender, times(2)).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
        verify(crawledArticleRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("API 호출 실패 시 예외 없이 빈 결과를 반환한다")
    void crawl_apiFailure_handlesGracefully() {
        given(naverNewsConfig.getKeywords()).willReturn("주식");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class))
                .willThrow(new RuntimeException("Connection refused"));

        naverNewsCrawlService.crawl();

        verify(aiDocumentSender, never()).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("빈 키워드는 무시하고 유효한 키워드만 크롤링한다")
    void crawl_blankKeywords_areSkipped() {
        given(naverNewsConfig.getKeywords()).willReturn("주식, ,코스피,,");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item1 = createNewsItem("주식 뉴스", "https://link1", "https://original1");
        NaverNewsItem item2 = createNewsItem("코스피 뉴스", "https://link2", "https://original2");
        NaverNewsApiResponse response1 = new NaverNewsApiResponse(null, 1, 1, 10, List.of(item1));
        NaverNewsApiResponse response2 = new NaverNewsApiResponse(null, 1, 1, 10, List.of(item2));

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class))
                .willReturn(response1)
                .willReturn(response2);
        given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
        given(aiDocumentSender.send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList())).willReturn(true);

        naverNewsCrawlService.crawl();

        verify(aiDocumentSender, times(2)).send(org.mockito.ArgumentMatchers.<DocumentRequest>anyList());
        verify(crawledArticleRepository, times(2)).saveAll(anyList());
    }
}
