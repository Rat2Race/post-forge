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

        List<NewsSourceItem> result = client.search(new NewsSourceQuery("커피", DISPLAY_COUNT, "date"));

        assertThat(result)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(DISPLAY_COUNT);

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
