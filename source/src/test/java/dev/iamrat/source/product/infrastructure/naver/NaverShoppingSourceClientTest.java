package dev.iamrat.source.product.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.iamrat.source.infrastructure.naver.NaverSearchMetrics;
import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.application.ProductSourceResult;
import dev.iamrat.source.product.domain.SourceType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NaverShoppingSourceClientTest {

    @Test
    @DisplayName("네이버 쇼핑 API를 호출하고 상품 항목으로 매핑한다")
    void callsNaverShoppingApiAndMapsProductItems() {
        NaverShoppingProperties properties = enabledProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverShoppingSourceClient client = new NaverShoppingSourceClient(
            properties,
            builder.build(),
            new NaverSearchMetrics(meterRegistry)
        );
        server.expect(requestTo(startsWith("https://openapi.naver.com/v1/search/shop.json")))
            .andExpect(queryParam("query", URLEncoder.encode("노트북", StandardCharsets.UTF_8)))
            .andExpect(queryParam("display", "5"))
            .andExpect(queryParam("start", "1"))
            .andExpect(queryParam("sort", "sim"))
            .andExpect(queryParam("exclude", "used:rental:cbshop"))
            .andExpect(header("X-Naver-Client-Id", "client-id"))
            .andExpect(header("X-Naver-Client-Secret", "client-secret"))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "productId": "123456",
                      "title": "<b>맥북</b> Air",
                      "link": "https://shopping.naver.com/catalog/123456",
                      "image": "https://img.example.com/123456.png",
                      "lprice": "1234000",
                      "mallName": "네이버",
                      "maker": "Apple",
                      "brand": "Apple",
                      "category1": "디지털/가전",
                      "category2": "노트북",
                      "category3": "맥북"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        ProductSourceResult result = client.search(new ProductSourceQuery(SourceType.NAVER, "노트북", 5));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().externalProductId()).isEqualTo("123456");
        assertThat(result.items().getFirst().title()).isEqualTo("맥북 Air");
        assertThat(result.items().getFirst().price()).isEqualTo(1_234_000L);
        assertThat(result.items().getFirst().category3()).isEqualTo("맥북");
        assertThat(meterRegistry.counter("external_naver_fetch_success_total", "api", "shopping").count())
            .isEqualTo(1.0);
        assertThat(meterRegistry.counter("external_naver_fetch_items_total", "api", "shopping").count())
            .isEqualTo(1.0);
        assertThat(meterRegistry.find("external_naver_fetch")
            .tag("api", "shopping")
            .tag("outcome", "success")
            .tag("status", "200")
            .timer()
            .count()).isEqualTo(1);
        server.verify();
    }

    @Test
    @DisplayName("네이버 쇼핑 인증값이 없으면 빠르게 실패한다")
    void failsFastWhenNaverShoppingCredentialsAreMissing() {
        NaverShoppingProperties properties = new NaverShoppingProperties();
        properties.setEnabled(true);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        NaverShoppingSourceClient client = new NaverShoppingSourceClient(
            properties,
            RestClient.builder().baseUrl(properties.getBaseUrl()).build(),
            new NaverSearchMetrics(meterRegistry)
        );

        assertThatThrownBy(() -> client.search(new ProductSourceQuery(SourceType.NAVER, "노트북", 5)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NAVER_SEARCH_CLIENT_ID");
        assertThat(meterRegistry.counter(
            "external_naver_fetch_failure_total",
            "api",
            "shopping",
            "exception",
            "IllegalStateException",
            "status",
            "none"
        ).count()).isEqualTo(1.0);
    }

    private NaverShoppingProperties enabledProperties() {
        NaverShoppingProperties properties = new NaverShoppingProperties();
        properties.setEnabled(true);
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        return properties;
    }
}
