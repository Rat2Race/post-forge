package dev.iamrat.source.news.infrastructure.naver;

import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

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
            .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
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

        List<NewsSourceItem> result = client.search(new NewsSourceQuery("갤럭시북 출시", 3, "date")).items();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("갤럭시북 신제품 출시");
        assertThat(result.getFirst().description()).isEqualTo("갤럭시북 신제품이 공개됐다.");
        assertThat(result.getFirst().rawTitle()).isEqualTo("<b>갤럭시북</b> 신제품 출시");
        assertThat(result.getFirst().rawDescription()).isEqualTo("<b>갤럭시북</b> 신제품이 공개됐다.");
        assertThat(result.getFirst().link()).isEqualTo("https://n.news.naver.com/article/001/0000000001");
        assertThat(result.getFirst().originalLink()).isEqualTo("https://news.example.com/original");
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

        assertThatThrownBy(() -> client.search(new NewsSourceQuery("갤럭시북", 3, "date")).items())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Naver News 소스를 사용하려면 NAVER_NEWS_ENABLED=true와 "
                + "NAVER_API_HUB_API_KEY_ID/NAVER_API_HUB_API_KEY 설정이 필요합니다");
    }

    @Test
    @DisplayName("link가 공백이면 originallink로 대체한다")
    void fallsBackToOriginalLinkWhenLinkIsBlank() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "갤럭시북 출시",
                      "originallink": "https://news.example.com/original",
                      "link": " ",
                      "description": "설명",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().link()).isEqualTo("https://news.example.com/original");
    }

    @Test
    @DisplayName("link와 originallink가 모두 공백이면 항목을 버린다")
    void dropsItemWhenBothLinksAreBlank() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "갤럭시북 출시",
                      "originallink": "",
                      "link": "",
                      "description": "설명",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("태그 제거 후 제목이 공백이면 항목을 버린다")
    void dropsItemWhenTitleIsBlankAfterCleaning() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "<b> </b>",
                      "originallink": "https://news.example.com/original",
                      "link": "https://n.news.naver.com/article/001/0000000001",
                      "description": "설명",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("문자열 필드는 저장 길이로 자르고 URL은 유효한 원문 링크로 대체한다")
    void truncatesFieldsToMaxLengths() {
        NaverNewsSourceClientFixture fixture = fixture();
        String longTitle = "a".repeat(250);
        String longDescription = "b".repeat(1100);
        String longPubDate = "c".repeat(150);
        String longLink = "https://n.news.naver.com/" + "d".repeat(1100);
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "%s",
                      "originallink": "https://news.example.com/original",
                      "link": "%s",
                      "description": "%s",
                      "pubDate": "%s"
                    }
                  ]
                }
                """.formatted(longTitle, longLink, longDescription, longPubDate), MediaType.APPLICATION_JSON));

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).hasSize(200);
        assertThat(result.getFirst().description()).hasSize(1000);
        assertThat(result.getFirst().publishedAt()).hasSize(100);
        assertThat(result.getFirst().link()).isEqualTo("https://news.example.com/original");
        assertThat(result.getFirst().originalLink()).isEqualTo("https://news.example.com/original");
    }

    @Test
    @DisplayName("키워드의 + 기호를 %2B로 인코딩한다")
    void encodesPlusSignInKeyword() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andExpect(queryParam("query", "%EA%B0%A4%EB%9F%AD%EC%8B%9C%20S25%2B"))
            .andRespond(withSuccess("{\"items\": []}", MediaType.APPLICATION_JSON));

        fixture.client().search(new NewsSourceQuery("갤럭시 S25+", 3, "date")).items();

        fixture.server().verify();
    }

    @Test
    @DisplayName("키워드에 중괄호가 있어도 URI 템플릿으로 해석하지 않는다")
    void doesNotTreatBracesInKeywordAsUriTemplate() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("{\"items\": []}", MediaType.APPLICATION_JSON));

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시{북}", 3, "date")).items();

        assertThat(result).isEmpty();
        fixture.server().verify();
    }

    @Test
    @DisplayName("&apos; 엔티티를 작은따옴표로 복원한다")
    void unescapesAposEntity() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "삼성 &apos;갤럭시북&apos; 출시",
                      "originallink": "https://news.example.com/original",
                      "link": "https://n.news.naver.com/article/001/0000000001",
                      "description": "&quot;신제품&quot; &amp; &apos;신기술&apos;",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items();

        assertThat(result.getFirst().title()).isEqualTo("삼성 '갤럭시북' 출시");
        assertThat(result.getFirst().description()).isEqualTo("\"신제품\" & '신기술'");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "<b>갤럭시북</b> &lt;프로&gt; 출시|갤럭시북 <프로> 출시",
        "&lt;strong&gt;Galaxy&lt;/strong&gt; &lt;Pro&gt; launch|Galaxy <Pro> launch",
        "<em>AI</em> &lt;Preview&gt;|AI <Preview>"
    })
    @DisplayName("알려진 강조 태그만 제거하고 한글과 영문 꺾쇠 리터럴은 남긴다")
    void stripsKnownEmphasisTagsAndKeepsLiteralAngleText(String title, String expectedTitle) {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "%s",
                      "originallink": "https://news.example.com/original",
                      "link": "https://n.news.naver.com/article/001/0000000001",
                      "description": "설명",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """.formatted(title), MediaType.APPLICATION_JSON));

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items();

        assertThat(result.getFirst().title()).isEqualTo(expectedTitle);
    }

    @Test
    @DisplayName("이스케이프된 script 마크업 제거는 유지한다")
    void stripsEscapedScriptMarkup() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "갤럭시북 출시",
                      "originallink": "https://news.example.com/original",
                      "link": "https://n.news.naver.com/article/001/0000000001",
                      "description": "&lt;script&gt;alert(1)&lt;/script&gt; 신제품",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items();

        assertThat(result.getFirst().description()).isEqualTo("alert(1) 신제품");
    }

    @ParameterizedTest
    @MethodSource("invalidProviderUrls")
    @DisplayName("유효하지 않은 provider URL은 자르지 않고 유효한 원문 URL로 대체한다")
    void fallsBackToValidOriginalUrl(String providerUrl) {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "갤럭시북 출시",
                      "originallink": "https://news.example.com/original?keep=raw",
                      "link": "%s",
                      "description": "설명",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """.formatted(providerUrl), MediaType.APPLICATION_JSON));

        NewsSourceItem item = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items().getFirst();

        assertThat(item.link()).isEqualTo("https://news.example.com/original?keep=raw");
        assertThat(item.originalLink()).isEqualTo("https://news.example.com/original?keep=raw");
    }

    @Test
    @DisplayName("문자열 길이 제한에서 UTF-16 surrogate pair를 나누지 않는다")
    void doesNotSplitSurrogatePairWhenTruncating() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess("""
                {
                  "items": [
                    {
                      "title": "%s",
                      "originallink": "https://news.example.com/original",
                      "link": "https://n.news.naver.com/article/001/1",
                      "description": "설명",
                      "pubDate": "Mon, 08 Jun 2026 10:00:00 +0900"
                    }
                  ]
                }
                """.formatted("가".repeat(199) + "😀뒤"), MediaType.APPLICATION_JSON));

        String title = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items().getFirst().title();

        assertThat(title).isEqualTo("가".repeat(199));
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500})
    @DisplayName("provider HTTP 실패 상태를 그대로 전파한다")
    void propagatesProviderHttpFailure(int status) {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withStatus(HttpStatusCode.valueOf(status)));

        RestClientResponseException thrown = catchThrowableOfType(
            RestClientResponseException.class,
            () -> fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items()
        );

        assertThat(thrown.getStatusCode().value()).isEqualTo(status);
    }

    @Test
    @DisplayName("응답 본문이 없으면 빈 결과를 반환한다")
    void returnsEmptyResultWhenResponseBodyIsNull() {
        NaverNewsSourceClientFixture fixture = fixture();
        fixture.server().expect(requestTo(startsWith("https://naverapihub.apigw.ntruss.com/search/v1/news")))
            .andRespond(withSuccess());

        List<NewsSourceItem> result = fixture.client().search(new NewsSourceQuery("갤럭시북", 3, "date")).items();

        assertThat(result).isEmpty();
    }

    private NaverNewsSourceClientFixture fixture() {
        NaverNewsProperties properties = enabledProperties();
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverNewsSourceClient client = new NaverNewsSourceClient(
            properties,
            builder.build(),
            new NaverNewsMetrics(new SimpleMeterRegistry())
        );
        return new NaverNewsSourceClientFixture(client, server);
    }

    private static Stream<Arguments> invalidProviderUrls() {
        return Stream.of(
            Arguments.of("https://n.news.naver.com/" + "a".repeat(1000)),
            Arguments.of("https://bad host/news"),
            Arguments.of("https://news.example.com/article?id=%"),
            Arguments.of("ftp://news.example.com/article")
        );
    }

    private record NaverNewsSourceClientFixture(
        NaverNewsSourceClient client,
        MockRestServiceServer server
    ) {
    }

    private NaverNewsProperties enabledProperties() {
        NaverNewsProperties properties = new NaverNewsProperties();
        properties.setEnabled(true);
        properties.setApiKeyId("api-key-id");
        properties.setApiKey("api-key");
        return properties;
    }
}
