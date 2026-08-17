package dev.iamrat.board.post.application;

import dev.iamrat.core.board.post.PostReferenceLinkReader;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPostReferenceLinkReader implements PostReferenceLinkReader {

    private final PostReferenceLinkStore postReferenceLinkStore;

    @Override
    public boolean existsByCanonicalUrl(String canonicalUrl) {
        return postReferenceLinkStore.existsByCanonicalUrl(canonicalUrl);
    }

    @Override
    public long countByKeywordAndProductIdOnDate(String keyword, Long productId, LocalDate publishedDate) {
        return postReferenceLinkStore.countByKeywordAndProductIdOnDate(keyword, productId, publishedDate);
    }
}
