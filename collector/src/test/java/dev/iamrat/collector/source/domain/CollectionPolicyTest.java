package dev.iamrat.collector.source.domain;

import dev.iamrat.collector.item.domain.CollectedArticle;
import dev.iamrat.collector.item.domain.CollectedItem;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionPolicyTest {

    private final CollectionPolicy collectionPolicy = new CollectionPolicy();

    @Test
    void incomingOriginalLinks_collectsLinksForLookup() {
        List<CollectedItem> items = List.of(
            newsItem("https://original-1"),
            newsItem("https://original-2"),
            newsItem("https://original-1")
        );

        Set<String> links = collectionPolicy.incomingOriginalLinks(items);

        assertThat(links).containsExactlyInAnyOrder("https://original-1", "https://original-2");
    }

    @Test
    void retainNewItems_excludesAlreadyCollectedArticles() {
        List<CollectedItem> items = List.of(
            newsItem("https://original-1"),
            newsItem("https://original-2")
        );
        List<CollectedArticle> existingArticles = List.of(article("https://original-1"));

        List<CollectedItem> newItems = collectionPolicy.retainNewItems(items, existingArticles);

        assertThat(newItems)
            .extracting(CollectedItem::originalLink)
            .containsExactly("https://original-2");
    }

    @Test
    void retainNewItems_returnsAllItemsWhenNoExistingArticles() {
        List<CollectedItem> items = List.of(
            newsItem("https://original-1"),
            newsItem("https://original-2")
        );

        List<CollectedItem> newItems = collectionPolicy.retainNewItems(items, List.of());

        assertThat(newItems).containsExactlyElementsOf(items);
    }

    private CollectedItem newsItem(String originalLink) {
        return new CollectedItem(
            "title",
            originalLink,
            "https://link",
            "description",
            "Mon, 15 Mar 2026 09:00:00 +0900"
        );
    }

    private CollectedArticle article(String originalLink) {
        return CollectedArticle.builder()
            .originalLink(originalLink)
            .title("title")
            .source("NAVER_NEWS")
            .keyword("keyword")
            .publishedAt("Mon, 15 Mar 2026 09:00:00 +0900")
            .build();
    }
}
