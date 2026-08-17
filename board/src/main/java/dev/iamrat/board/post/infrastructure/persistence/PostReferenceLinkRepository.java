package dev.iamrat.board.post.infrastructure.persistence;

import dev.iamrat.board.post.domain.PostReferenceLink;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReferenceLinkRepository extends JpaRepository<PostReferenceLink, Long> {

    boolean existsByCanonicalUrl(String canonicalUrl);

    @Query("""
        SELECT COUNT(prl)
        FROM PostReferenceLink prl
        WHERE LOWER(prl.keyword) = :keyword
          AND ((:productId IS NULL AND prl.productId IS NULL) OR prl.productId = :productId)
          AND prl.publishedAt >= :startInclusive
          AND prl.publishedAt < :endExclusive
        """)
    long countByKeywordAndProductIdBetween(
        @Param("keyword") String keyword,
        @Param("productId") Long productId,
        @Param("startInclusive") LocalDateTime startInclusive,
        @Param("endExclusive") LocalDateTime endExclusive
    );

    List<PostReferenceLink> findByPost_Id(Long postId);

    List<PostReferenceLink> findByPost_IdInOrderByPost_IdAscIdAsc(List<Long> postIds);
}
