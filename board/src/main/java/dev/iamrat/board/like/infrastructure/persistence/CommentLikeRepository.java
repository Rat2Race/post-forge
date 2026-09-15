package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.like.domain.CommentLike;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByComment_IdAndAccountId(Long commentId, Long accountId);

    long countByComment_Id(Long commentId);

    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id IN :commentIds GROUP BY cl.comment.id")
    List<Object[]> countByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.accountId = :accountId AND cl.comment.id IN :commentIds")
    Set<Long> findLikedCommentIdsByAccountIdAndCommentIds(@Param("accountId") Long accountId, @Param("commentIds") List<Long> commentIds);

    @Modifying
    @Transactional
    long deleteByComment_IdAndAccountId(Long commentId, Long accountId);
}
