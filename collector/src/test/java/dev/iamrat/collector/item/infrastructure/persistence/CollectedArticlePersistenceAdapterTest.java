package dev.iamrat.collector.item.infrastructure.persistence;

import dev.iamrat.collector.item.domain.CollectedArticle;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CollectedArticlePersistenceAdapterTest {

    private final CollectedArticleRepository collectedArticleRepository =
        mock(CollectedArticleRepository.class);
    private final CollectedArticlePersistenceAdapter adapter =
        new CollectedArticlePersistenceAdapter(collectedArticleRepository);

    @Test
    @DisplayName("originalLink 목록으로 기존 수집 기사를 조회한다")
    void findByOriginalLinkIn_delegatesToRepository() {
        Set<String> originalLinks = Set.of("https://news.example/original");
        CollectedArticle article = collectedArticle("https://news.example/original");
        given(collectedArticleRepository.findByOriginalLinkIn(originalLinks))
            .willReturn(List.of(article));

        List<CollectedArticle> result = adapter.findByOriginalLinkIn(originalLinks);

        assertThat(result).containsExactly(article);
        verify(collectedArticleRepository).findByOriginalLinkIn(originalLinks);
    }

    @Test
    @DisplayName("수집 기사를 JPA repository에 일괄 저장한다")
    void saveAll_delegatesToRepository() {
        CollectedArticle article = collectedArticle("https://news.example/original");
        Collection<CollectedArticle> articles = List.of(article);
        given(collectedArticleRepository.saveAll(articles)).willReturn(List.of(article));

        List<CollectedArticle> result = adapter.saveAll(articles);

        assertThat(result).containsExactly(article);
        verify(collectedArticleRepository).saveAll(articles);
    }

    private CollectedArticle collectedArticle(String originalLink) {
        return CollectedArticle.builder()
            .originalLink(originalLink)
            .title("수집 기사")
            .source("NAVER_NEWS")
            .keyword("AI")
            .publishedAt("Mon, 15 Mar 2026 09:00:00 +0900")
            .build();
    }
}
