package dev.iamrat.source.news.infrastructure.googlenews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;
import java.util.Map;

class GoogleNewsRssSourceClientTest {

    private static final String LINK_1 = "https://news.google.com/rss/articles/AAA?oc=5";
    private static final String LINK_2 = "https://news.google.com/rss/articles/BBB?oc=5";
    private static final String LINK_3 = "https://news.google.com/rss/articles/CCC?oc=5";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://news.google.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    @DisplayName("검색 피드를 호출하고 제목의 출처 접미사를 떼어 최신순으로 displayCount만큼 돌려준다")
    void searchesFeedAndMapsNewestFirst() {
        GoogleNewsRssSourceClient client = client(true);
        server.expect(requestTo(startsWith("https://news.google.com/rss/search")))
            .andExpect(queryParam("q", UriUtils.encodeQueryParam("갤럭시 출시", StandardCharsets.UTF_8)))
            .andExpect(queryParam("hl", "ko"))
            .andExpect(queryParam("gl", "KR"))
            .andExpect(queryParam("ceid", "KR:ko"))
            .andExpect(header("User-Agent", startsWith("PostForge/")))
            .andRespond(withSuccess(rss(
                item("오래된 갤럭시 북6 출시 - v.daum.net", LINK_1, "v.daum.net", "https://v.daum.net", "Mon, 28 Sep 2026 09:00:00 GMT"),
                item("최신 <b>갤럭시</b> S26 출시 - 전자신문", LINK_2, "전자신문", "https://www.etnews.com", "Tue, 29 Sep 2026 19:33:00 GMT"),
                item("중간 갤럭시 워치 출시 - ZDNet Korea", LINK_3, "ZDNet Korea", "https://zdnet.co.kr", "Tue, 29 Sep 2026 10:00:00 GMT")
            ), MediaType.APPLICATION_RSS_XML));

        List<NewsSourceItem> result = client.search(new NewsSourceQuery("갤럭시 출시", 2, "date"));

        assertThat(result).extracting(NewsSourceItem::title).containsExactly("최신 갤럭시 S26 출시", "중간 갤럭시 워치 출시");
        NewsSourceItem first = result.getFirst();
        assertThat(first.link()).isEqualTo(LINK_2);
        assertThat(first.originalLink()).isEqualTo(LINK_2);
        assertThat(first.publishedAt()).isEqualTo("Tue, 29 Sep 2026 19:33:00 GMT");
        assertThat(first.rawTitle()).isEqualTo("최신 <b>갤럭시</b> S26 출시 - 전자신문");
        assertThat(first.description()).isEqualTo("최신 갤럭시 S26 출시 (전자신문)");
        assertThat(registry.counter("external_news_fetch_success_total", "api", "news").count()).isEqualTo(1.0);
        assertThat(registry.counter("external_news_fetch_items_total", "api", "news").count()).isEqualTo(2.0);
        server.verify();
    }

    @Test
    @DisplayName("섹션 이름으로 매핑된 키워드는 검색 대신 주제 헤드라인 피드를 읽는다")
    void readsTopicFeedForMappedSection() {
        GoogleNewsRssProperties properties = new GoogleNewsRssProperties();
        properties.setEnabled(true);
        properties.setSections(Map.of("기술", "technology"));
        GoogleNewsRssSourceClient client = new GoogleNewsRssSourceClient(properties, builder.build(), new NewsFetchMetrics(registry));
        server.expect(requestTo("https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=ko&gl=KR&ceid=KR:ko"))
            .andRespond(withSuccess(rss(
                item("기술 헤드라인 - A", LINK_1, "A", "https://a.example", "Tue, 29 Sep 2026 09:00:00 GMT")
            ), MediaType.APPLICATION_RSS_XML));

        assertThat(client.search(new NewsSourceQuery("기술", 5, "date")))
            .extracting(NewsSourceItem::title).containsExactly("기술 헤드라인");
        server.verify();
    }

    @Test
    @DisplayName("섹션 값이 Google topics 식별자면 /rss/topics 피드를 읽는다")
    void readsTopicIdFeedForMappedSection() {
        GoogleNewsRssProperties properties = new GoogleNewsRssProperties();
        properties.setEnabled(true);
        properties.setSections(Map.of("과학기술", "CAAqKAgKIiJDQkFTRXdvSkwyMHZNR1ptZHpWbUVnSnJieG9DUzFJb0FBUAE"));
        GoogleNewsRssSourceClient client = new GoogleNewsRssSourceClient(properties, builder.build(), new NewsFetchMetrics(registry));
        server.expect(requestTo("https://news.google.com/rss/topics/CAAqKAgKIiJDQkFTRXdvSkwyMHZNR1ptZHpWbUVnSnJieG9DUzFJb0FBUAE?hl=ko&gl=KR&ceid=KR:ko"))
            .andRespond(withSuccess(rss(
                item("과학기술 헤드라인 - A", LINK_1, "A", "https://a.example", "Tue, 29 Sep 2026 09:00:00 GMT")
            ), MediaType.APPLICATION_RSS_XML));

        assertThat(client.search(new NewsSourceQuery("과학기술", 5, "date")))
            .extracting(NewsSourceItem::title).containsExactly("과학기술 헤드라인");
        server.verify();
    }

    @Test
    @DisplayName("sort=sim이면 피드 순서를 유지하고 같은 링크는 한 번만 남긴다")
    void keepsFeedOrderForSimilaritySortAndDedupesLinks() {
        GoogleNewsRssSourceClient client = client(true);
        server.expect(requestTo(startsWith("https://news.google.com/rss/search")))
            .andRespond(withSuccess(rss(
                item("첫 기사 - A", LINK_1, "A", "https://a.example", "Mon, 28 Sep 2026 09:00:00 GMT"),
                item("둘째 기사 - B", LINK_2, "B", "https://b.example", "Tue, 29 Sep 2026 09:00:00 GMT"),
                item("첫 기사 중복 - A", LINK_1, "A", "https://a.example", "Tue, 29 Sep 2026 12:00:00 GMT")
            ), MediaType.APPLICATION_RSS_XML));

        List<NewsSourceItem> result = client.search(new NewsSourceQuery("기사", 10, "sim"));

        assertThat(result).extracting(NewsSourceItem::title).containsExactly("첫 기사", "둘째 기사");
    }

    @Test
    @DisplayName("링크나 제목이 없는 항목은 버린다")
    void dropsItemsWithoutLinkOrTitle() {
        GoogleNewsRssSourceClient client = client(true);
        server.expect(requestTo(startsWith("https://news.google.com/rss/search")))
            .andRespond(withSuccess(rss(
                "<item><title>링크 없음</title><description>x</description></item>",
                "<item><link>" + LINK_1 + "</link><description>제목 없음</description></item>",
                item("정상 - A", LINK_2, "A", "https://a.example", "Tue, 29 Sep 2026 09:00:00 GMT")
            ), MediaType.APPLICATION_RSS_XML));

        assertThat(client.search(new NewsSourceQuery("기사", 10, "date")))
            .extracting(NewsSourceItem::title).containsExactly("정상");
    }

    @Test
    @DisplayName("HTTP 오류는 상태 코드와 함께 실패 메트릭을 남기고 예외를 던진다")
    void recordsFailureOnHttpError() {
        GoogleNewsRssSourceClient client = client(true);
        server.expect(requestTo(startsWith("https://news.google.com/rss/search"))).andRespond(withServerError());

        assertThatThrownBy(() -> client.search(new NewsSourceQuery("기사", 5, "date")))
            .isInstanceOf(RestClientResponseException.class);
        assertThat(registry.find("external_news_fetch_failure_total").tag("status", "500").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("external_news_fetch").tag("outcome", "failure").timer().count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("DOCTYPE이 있는 피드는 파싱을 거부한다")
    void rejectsDoctype() {
        GoogleNewsRssSourceClient client = client(true);
        server.expect(requestTo(startsWith("https://news.google.com/rss/search"))).andRespond(withSuccess(
            "<?xml version=\"1.0\"?><!DOCTYPE rss [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<rss><channel><item><title>&x;</title><link>https://a.example</link></item></channel></rss>",
            MediaType.APPLICATION_RSS_XML));

        assertThatThrownBy(() -> client.search(new NewsSourceQuery("기사", 5, "date")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Google News RSS 피드를 해석할 수 없습니다");
    }

    @Test
    @DisplayName("비활성이면 호출을 거부한다")
    void rejectsWhenDisabled() {
        assertThatThrownBy(() -> client(false).search(new NewsSourceQuery("기사", 5, "date")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Google News RSS 소스를 사용하려면 GOOGLE_NEWS_ENABLED=true 설정이 필요합니다");
    }

    private GoogleNewsRssSourceClient client(boolean enabled) {
        GoogleNewsRssProperties properties = new GoogleNewsRssProperties();
        properties.setEnabled(enabled);
        return new GoogleNewsRssSourceClient(properties, builder.build(), new NewsFetchMetrics(registry));
    }

    private static String rss(String... items) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><title>t</title>"
            + String.join("", items) + "</channel></rss>";
    }

    private static String item(String title, String link, String sourceName, String sourceUrl, String pubDate) {
        String escapedTitle = title.replace("<", "&lt;").replace(">", "&gt;");
        return "<item><title>" + escapedTitle + "</title><link>" + link + "</link>"
            + "<pubDate>" + pubDate + "</pubDate>"
            + "<description>&lt;a href=\"" + link + "\"&gt;" + escapedTitle + "&lt;/a&gt;&amp;nbsp;&amp;nbsp;"
            + "&lt;font color=\"#6f6f6f\"&gt;" + sourceName + "&lt;/font&gt;</description>"
            + "<source url=\"" + sourceUrl + "\">" + sourceName + "</source></item>";
    }
}
