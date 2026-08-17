package dev.iamrat.board.post.infrastructure.persistence;

import dev.iamrat.board.post.application.PostReferenceLinkStore;
import dev.iamrat.board.post.domain.PostReferenceLink;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostReferenceLinkPersistenceAdapter implements PostReferenceLinkStore {

    private final PostReferenceLinkRepository postReferenceLinkRepository;

    @Override
    public PostReferenceLink save(PostReferenceLink referenceLink) {
        return postReferenceLinkRepository.save(referenceLink);
    }

    @Override
    public boolean existsByCanonicalUrl(String canonicalUrl) {
        return postReferenceLinkRepository.existsByCanonicalUrl(canonicalUrl);
    }

    @Override
    public long countByKeywordAndProductIdOnDate(String keyword, Long productId, LocalDate publishedDate) {
        return postReferenceLinkRepository.countByKeywordAndProductIdBetween(
            normalizeKeyword(keyword),
            productId,
            publishedDate.atStartOfDay(),
            publishedDate.plusDays(1).atStartOfDay()
        );
    }

    @Override
    public List<PostReferenceLink> findByPostId(Long postId) {
        return postReferenceLinkRepository.findByPost_Id(postId);
    }

    @Override
    public List<PostReferenceLink> findByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        return postReferenceLinkRepository.findByPost_IdInOrderByPost_IdAscIdAsc(postIds);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
    }
}
