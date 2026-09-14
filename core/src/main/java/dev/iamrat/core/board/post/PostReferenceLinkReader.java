package dev.iamrat.core.board.post;

import java.time.LocalDate;

public interface PostReferenceLinkReader {
    boolean existsByCanonicalUrl(String canonicalUrl);

    long countByKeywordOnDate(String keyword, LocalDate publishedDate);
}
