package dev.iamrat.news.service;

import dev.iamrat.common.InternalCrawlClient;
import dev.iamrat.common.dto.InternalDocumentPayload;
import dev.iamrat.common.entity.CrawledArticle;
import dev.iamrat.common.repository.CrawledArticleRepository;
import dev.iamrat.news.config.NaverNewsConfig;
import dev.iamrat.news.dto.NaverNewsApiResponse;
import dev.iamrat.news.dto.NaverNewsItem;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NaverNewsCrawlServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient naverNewsRestClient;

    @Mock
    private NaverNewsConfig naverNewsConfig;

    @Mock
    private CrawledArticleRepository crawledArticleRepository;

    @Mock
    private InternalCrawlClient internalCrawlClient;

    @InjectMocks
    private NaverNewsCrawlService naverNewsCrawlService;

    private NaverNewsItem createNewsItem(String title, String link, String originalLink) {
        return new NaverNewsItem(title, originalLink, link, "뉴스 내용 요약",
                "Mon, 15 Mar 2026 09:00:00 +0900");
    }

    @Test
    @DisplayName("새 뉴스가 있으면 DB에 먼저 저장하고 메인 앱으로 전송한다")
    void crawl_newNews_savesThenSends() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
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
        given(internalCrawlClient.sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList())).willReturn(true);

        naverNewsCrawlService.crawl();

        InOrder inOrder = inOrder(crawledArticleRepository, internalCrawlClient);
        inOrder.verify(crawledArticleRepository).saveAll(anyList());
        inOrder.verify(internalCrawlClient).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
    }

    @Test
    @DisplayName("메인 앱 전송 실패 시에도 H2 저장은 유지한다")
    void crawl_sendFails_keepsSavedArticles() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
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
        given(internalCrawlClient.sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList())).willReturn(false);

        naverNewsCrawlService.crawl();

        verify(crawledArticleRepository).saveAll(anyList());
        verify(internalCrawlClient).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
    }

    @Test
    @DisplayName("이미 저장된 기사는 필터링한다")
    void crawl_duplicateArticles_filtered() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
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

        verify(internalCrawlClient, never()).sendDocuments(anyList());
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("여러 키워드를 순회하며 크롤링한다")
    void crawl_multipleKeywords_crawlsAll() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
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
        given(internalCrawlClient.sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList())).willReturn(true);

        naverNewsCrawlService.crawl();

        verify(internalCrawlClient, times(2)).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
        verify(crawledArticleRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("API 호출 실패 시 예외 없이 빈 결과를 반환한다")
    void crawl_apiFailure_handlesGracefully() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
        given(naverNewsConfig.getKeywords()).willReturn("주식");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class))
                .willThrow(new RuntimeException("Connection refused"));

        naverNewsCrawlService.crawl();

        verify(internalCrawlClient, never()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("빈 키워드는 무시하고 유효한 키워드만 크롤링한다")
    void crawl_blankKeywords_areSkipped() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
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
        given(internalCrawlClient.sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList())).willReturn(true);

        naverNewsCrawlService.crawl();

        verify(internalCrawlClient, times(2)).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
        verify(crawledArticleRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("설정이 비어 있으면 네이버 뉴스 크롤링을 건너뛴다")
    void crawl_missingCredentials_skips() {
        given(naverNewsConfig.getClientId()).willReturn("");

        naverNewsCrawlService.crawl();

        verify(naverNewsRestClient, never()).get();
        verify(internalCrawlClient, never()).sendDocuments(org.mockito.ArgumentMatchers.<InternalDocumentPayload>anyList());
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }
}

