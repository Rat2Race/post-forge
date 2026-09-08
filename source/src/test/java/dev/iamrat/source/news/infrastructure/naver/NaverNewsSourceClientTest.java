package dev.iamrat.source.news.infrastructure.naver;

import dev.iamrat.source.news.infrastructure.naver.NaverNewsMetrics;
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
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverNewsSourceClient client = new NaverNewsSourceClient(
            properties,
            builder.build(),
            new NaverNewsMetrics(new SimpleMeterRegistry())
        );
        server.expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andExpect(queryParam("query", UriUtils.encodeQueryParam("갤럭시북 출시", StandardCharsets.UTF_8)))
            .andExpect(queryParam("display", "3"))
            .andExpect(queryParam("start", "1"))
            .andExpect(queryParam("sort", "date"))
            .andExpect(header("X-NCP-APIGW-API-KEY-ID", "api-key-id"))
            .andExpect(header("X-NCP-APIGW-API-KEY", "api-key"))
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
        server.verify();
    }

    @Test
    @DisplayName("네이버 뉴스 인증값이 없으면 빠르게 실패한다")
    void failsFastWhenNaverNewsCredentialsAreMissing() {
        NaverNewsProperties properties = new NaverNewsProperties();
        properties.setEnabled(true);
        NaverNewsSourceClient client = new NaverNewsSourceClient(
            properties,
            RestClient.builder().baseUrl(properties.getBaseUrl()).build(),
            new NaverNewsMetrics(new SimpleMeterRegistry())
        );

        assertThatThrownBy(() -> client.search(new NewsSourceQuery("갤럭시북", 3, "date")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NAVER_API_HUB_API_KEY_ID");

    }

    private NaverNewsProperties enabledProperties() {
        NaverNewsProperties properties = new NaverNewsProperties();
        properties.setEnabled(true);
        properties.setApiKeyId("api-key-id");
        properties.setApiKey("api-key");
        return properties;
    }
}
