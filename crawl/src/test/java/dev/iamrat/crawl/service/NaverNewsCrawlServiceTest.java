package dev.iamrat.crawl.service;

import dev.iamrat.crawl.common.AiDocumentSender;
import dev.iamrat.crawl.config.NaverNewsConfig;
import dev.iamrat.crawl.dto.NaverNewsApiResponse;
import dev.iamrat.crawl.dto.NaverNewsItem;
import dev.iamrat.crawl.entity.CrawledArticle;
import dev.iamrat.crawl.repository.CrawledArticleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
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
    @DisplayName("새 뉴스가 있으면 AI 전송 후 DB에 저장한다")
    void crawl_newNews_sendsToAiAndSaves() {
        // given
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
        given(aiDocumentSender.send(anyList())).willReturn(true);

        // when
        naverNewsCrawlService.crawl();

        // then
        verify(aiDocumentSender).send(anyList());
        verify(crawledArticleRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("AI 전송 실패 시 DB에 저장하지 않는다")
    void crawl_aiSendFails_doesNotSave() {
        // given
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
        given(aiDocumentSender.send(anyList())).willReturn(false);

        // when
        naverNewsCrawlService.crawl();

        // then
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("이미 저장된 기사는 필터링한다")
    void crawl_duplicateArticles_filtered() {
        // given
        given(naverNewsConfig.getKeywords()).willReturn("주식");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item = createNewsItem(
                "삼성전자 주가 상승", "https://link", "https://original-link");
        NaverNewsApiResponse response = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item));

        CrawledArticle existing = CrawledArticle.builder()
                .originalLink("https://original-link")
                .build();

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class)).willReturn(response);
        given(crawledArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of(existing));

        // when
        naverNewsCrawlService.crawl();

        // then
        verify(aiDocumentSender, never()).send(anyList());
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("여러 키워드를 순회하며 크롤링한다")
    void crawl_multipleKeywords_crawlsAll() {
        // given
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
        given(aiDocumentSender.send(anyList())).willReturn(true);

        // when
        naverNewsCrawlService.crawl();

        // then
        verify(aiDocumentSender, times(2)).send(anyList());
        verify(crawledArticleRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("API 호출 실패 시 예외 없이 빈 결과를 반환한다")
    void crawl_apiFailure_handlesGracefully() {
        // given
        given(naverNewsConfig.getKeywords()).willReturn("주식");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class))
                .willThrow(new RuntimeException("Connection refused"));

        // when
        naverNewsCrawlService.crawl();

        // then
        verify(aiDocumentSender, never()).send(anyList());
        verify(crawledArticleRepository, never()).saveAll(anyList());
    }
}
