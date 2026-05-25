package dev.iamrat.collector.source.domain;

import dev.iamrat.collector.item.domain.CollectedArticle;
import dev.iamrat.collector.item.domain.CollectedItem;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionPolicy {

    public Set<String> incomingOriginalLinks(List<CollectedItem> items) {
        return items.stream()
            .map(CollectedItem::originalLink)
            .collect(Collectors.toSet());
    }

    public List<CollectedItem> retainNewItems(List<CollectedItem> items, List<CollectedArticle> existingArticles) {
        Set<String> existingLinks = existingArticles.stream()
            .map(CollectedArticle::getOriginalLink)
            .collect(Collectors.toSet());

        return items.stream()
            .filter(item -> !existingLinks.contains(item.originalLink()))
            .toList();
    }
}
