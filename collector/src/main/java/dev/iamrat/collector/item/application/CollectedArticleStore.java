package dev.iamrat.collector.item.application;

import dev.iamrat.collector.item.domain.CollectedArticle;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CollectedArticleStore {
    List<CollectedArticle> findByOriginalLinkIn(Set<String> originalLinks);

    List<CollectedArticle> saveAll(Collection<CollectedArticle> articles);
}
