package dev.iamrat.source.news.infrastructure.naver;

import dev.iamrat.source.infrastructure.naver.NaverSearchMetrics;
import dev.iamrat.source.news.application.NewsSourceQuery;
import dev.iamrat.source.news.application.NewsSourceResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverNewsSourceClientTest {

    @Test
    @DisplayName("네이버 뉴스 API를 호출하고 뉴스 항목으로 매핑한다")
    void callsNaverNewsApiAndMapsNewsItems() {
        NaverNewsProperties properties = enabledProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverNewsSourceClient client = new NaverNewsSourceClient(
            properties,
            builder.build(),
            new NaverSearchMetrics(meterRegistry)
        );
        server.expect(requestTo(startsWith("https://openapi.naver.com/v1/search/news.json")))
            .andExpect(queryParam("query", UriUtils.encodeQueryParam("갤럭시북 출시", StandardCharsets.UTF_8)))
            .andExpect(queryParam("display", "3"))
            .andExpect(queryParam("start", "1"))
            .andExpect(queryParam("sort", "date"))
            .andExpect(header("X-Naver-Client-Id", "client-id"))
            .andExpect(header("X-Naver-Client-Secret", "client-secret"))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "<b>갤럭시북</b> 신제품 출시",
                      "originallink": "https://news.example.com/original",
                      "link": "https://n.news.naver.com/article/001/0000000001",
                      "description": "<b>갤럭시북</b> 신제품이 공개됐다.",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        NewsSourceResult result = client.search(new NewsSourceQuery("갤럭시북 출시", 3, "date"));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().title()).isEqualTo("갤럭시북 신제품 출시");
        assertThat(result.items().getFirst().description()).isEqualTo("갤럭시북 신제품이 공개됐다.");
        assertThat(result.items().getFirst().rawTitle()).isEqualTo("<b>갤럭시북</b> 신제품 출시");
        assertThat(result.items().getFirst().rawDescription()).isEqualTo("<b>갤럭시북</b> 신제품이 공개됐다.");
        assertThat(result.items().getFirst().link()).isEqualTo("https://n.news.naver.com/article/001/0000000001");
        assertThat(result.items().getFirst().originalLink()).isEqualTo("https://news.example.com/original");
        assertThat(meterRegistry.counter("external_naver_fetch_success_total", "api", "news").count())
            .isEqualTo(1.0);
        assertThat(meterRegistry.counter("external_naver_fetch_items_total", "api", "news").count())
            .isEqualTo(1.0);
        assertThat(meterRegistry.find("external_naver_fetch")
            .tag("api", "news")
            .tag("outcome", "success")
            .tag("status", "200")
            .timer()
            .count()).isEqualTo(1);
        server.verify();
    }

    @Test
    @DisplayName("네이버 뉴스 인증값이 없으면 빠르게 실패한다")
    void failsFastWhenNaverNewsCredentialsAreMissing() {
        NaverNewsProperties properties = new NaverNewsProperties();
        properties.setEnabled(true);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        NaverNewsSourceClient client = new NaverNewsSourceClient(
            properties,
            RestClient.builder().baseUrl(properties.getBaseUrl()).build(),
            new NaverSearchMetrics(meterRegistry)
        );

        assertThatThrownBy(() -> client.search(new NewsSourceQuery("갤럭시북", 3, "date")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NAVER_SEARCH_CLIENT_ID");
        assertThat(meterRegistry.counter(
            "external_naver_fetch_failure_total",
            "api",
            "news",
            "exception",
            "IllegalStateException",
            "status",
            "none"
        ).count()).isEqualTo(1.0);
    }

    private NaverNewsProperties enabledProperties() {
        NaverNewsProperties properties = new NaverNewsProperties();
        properties.setEnabled(true);
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        return properties;
    }
}
