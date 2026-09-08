package dev.iamrat.source.news.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@Tag("integration")
@EnabledIfSystemProperty(named = "naver.smoke", matches = "true")
@EnabledIfEnvironmentVariable(named = "NAVER_API_HUB_API_KEY_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "NAVER_API_HUB_API_KEY", matches = ".+")
class NaverNewsSmokeTest {

    private static final int DISPLAY_COUNT = 3;

    @Test
    @DisplayName("실서버 응답이 클라이언트의 매핑 전제와 일치한다")
    void realResponseMatchesClientAssumptions() {
        NaverNewsSourceClient client = new NaverNewsSourceClient(
            liveProperties(),
            new NaverNewsMetrics(new SimpleMeterRegistry())
        );

        List<NewsSourceItem> result = client.search(new NewsSourceQuery("커피", DISPLAY_COUNT, "date")).items();

        // 응답이 text/plain으로 오면 여기 도달하기 전에 UnknownContentTypeException으로 실패한다.
        // 비어 있다면 items 래퍼 키나 항목 필드명이 바뀐 것이다. 예외 없이 빈 성공으로 흘러가는 결함이라 여기서 막는다.
        assertThat(result)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(DISPLAY_COUNT);

        // pubDate 포맷이 바뀌면 ingest의 발행일 파싱이 null을 반환하고 현재 시각으로 대체돼
        // 일일 발행 상한의 날짜 키가 조용히 오염된다.
        assertThat(result).allSatisfy(item ->
            assertThatCode(() -> ZonedDateTime.parse(item.publishedAt(), DateTimeFormatter.RFC_1123_DATE_TIME))
                .doesNotThrowAnyException());
    }

    private NaverNewsProperties liveProperties() {
        NaverNewsProperties properties = new NaverNewsProperties();
        properties.setEnabled(true);
        properties.setApiKeyId(System.getenv("NAVER_API_HUB_API_KEY_ID"));
        properties.setApiKey(System.getenv("NAVER_API_HUB_API_KEY"));
        return properties;
    }
}
