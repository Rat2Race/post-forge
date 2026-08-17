package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.PostReferenceLink;
import java.time.LocalDate;
import java.util.List;

public interface PostReferenceLinkStore {

    PostReferenceLink save(PostReferenceLink referenceLink);

    boolean existsByCanonicalUrl(String canonicalUrl);

    long countByKeywordAndProductIdOnDate(String keyword, Long productId, LocalDate publishedDate);

    List<PostReferenceLink> findByPostId(Long postId);

    List<PostReferenceLink> findByPostIds(List<Long> postIds);
}
