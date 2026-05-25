package dev.iamrat.collector.source.infrastructure.external.naver;

import dev.iamrat.collector.item.domain.CollectedItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NaverNewsApiClientTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient naverNewsRestClient;

    @Test
    @DisplayName("네이버 뉴스 API 응답 DTO를 collector domain item으로 변환한다")
    void search_mapsApiResponseToDomainItems() {
        NaverNewsApiClient client = new NaverNewsApiClient(naverNewsRestClient);
        NaverNewsItem apiItem = new NaverNewsItem(
            "AI 반도체 수요 증가",
            "https://original-link",
            "https://link",
            "뉴스 내용 요약",
            "Mon, 15 Mar 2026 09:00:00 +0900"
        );
        NaverNewsApiResponse response = new NaverNewsApiResponse(null, 1, 1, 10, List.of(apiItem));
        given(naverNewsRestClient.get()
            .uri(anyString(), any(), any())
            .retrieve()
            .body(NaverNewsApiResponse.class)).willReturn(response);

        List<CollectedItem> items = client.search("테크", 10);

        assertThat(items)
            .singleElement()
            .satisfies(item -> {
                assertThat(item.title()).isEqualTo("AI 반도체 수요 증가");
                assertThat(item.originalLink()).isEqualTo("https://original-link");
                assertThat(item.link()).isEqualTo("https://link");
                assertThat(item.description()).isEqualTo("뉴스 내용 요약");
                assertThat(item.publishedAt()).isEqualTo("Mon, 15 Mar 2026 09:00:00 +0900");
            });
    }

    @Test
    @DisplayName("네이버 뉴스 API 호출 실패는 빈 결과로 변환한다")
    void search_apiFailure_returnsEmptyResult() {
        NaverNewsApiClient client = new NaverNewsApiClient(naverNewsRestClient);
        given(naverNewsRestClient.get()
            .uri(anyString(), any(), any())
            .retrieve()
            .body(NaverNewsApiResponse.class)).willThrow(new RuntimeException("Connection refused"));

        List<CollectedItem> items = client.search("테크", 10);

        assertThat(items).isEmpty();
    }
}
