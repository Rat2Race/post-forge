package dev.iamrat.source.news.infrastructure.googlenews;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "google-news.smoke", matches = "true")
class GoogleNewsRssSmokeTest {

    @Test
    @DisplayName("Google News RSS 검색 피드를 실제로 읽는다")
    void readsLiveFeed() {
        GoogleNewsRssProperties properties = new GoogleNewsRssProperties();
        properties.setEnabled(true);
        GoogleNewsRssSourceClient client = new GoogleNewsRssSourceClient(properties, new NewsFetchMetrics(new SimpleMeterRegistry()));

        List<NewsSourceItem> items = client.search(new NewsSourceQuery("갤럭시 출시", 3, "date"));

        assertThat(items).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        assertThat(items.getFirst().link()).startsWith("https://news.google.com/rss/articles/");
        assertThat(items.getFirst().publishedAt()).isNotBlank();
    }
}
