package dev.iamrat.board.purchase.infrastructure.persistence;

import dev.iamrat.board.purchase.domain.PostPurchaseVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PurchaseVoteRepository extends JpaRepository<PostPurchaseVote, Long> {

    Optional<PostPurchaseVote> findByPost_IdAndAccountId(Long postId, Long accountId);

    List<PostPurchaseVote> findByAccountIdAndPost_IdIn(Long accountId, List<Long> postIds);

    @Query("""
        SELECT pv.post.id, pv.voteType, COUNT(pv)
        FROM PostPurchaseVote pv
        WHERE pv.post.id IN :postIds
        GROUP BY pv.post.id, pv.voteType
        """)
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Transactional
    long deleteByPost_IdAndAccountId(Long postId, Long accountId);
}
