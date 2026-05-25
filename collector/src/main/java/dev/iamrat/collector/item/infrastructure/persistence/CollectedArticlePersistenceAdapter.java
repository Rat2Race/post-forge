package dev.iamrat.collector.item.infrastructure.persistence;

import dev.iamrat.collector.item.application.CollectedArticleStore;
import dev.iamrat.collector.item.domain.CollectedArticle;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectedArticlePersistenceAdapter implements CollectedArticleStore {

    private final CollectedArticleRepository collectedArticleRepository;

    @Override
    public List<CollectedArticle> findByOriginalLinkIn(Set<String> originalLinks) {
        return collectedArticleRepository.findByOriginalLinkIn(originalLinks);
    }

    @Override
    public List<CollectedArticle> saveAll(Collection<CollectedArticle> articles) {
        return collectedArticleRepository.saveAll(articles);
    }
}
