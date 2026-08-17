package dev.iamrat.core.board.post;

import java.time.LocalDate;

public interface PostReferenceLinkReader {
    boolean existsByCanonicalUrl(String canonicalUrl);

    long countByKeywordAndProductIdOnDate(String keyword, Long productId, LocalDate publishedDate);
}
