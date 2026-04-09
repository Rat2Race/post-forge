package dev.iamrat.like.comment.repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import dev.iamrat.like.comment.entity.CommentLike;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByComment_IdAndUserId(Long commentId, String userId);

    long countByComment_Id(Long commentId);

    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id IN :commentIds GROUP BY cl.comment.id")
    List<Object[]> countByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.userId = :userId AND cl.comment.id IN :commentIds")
    Set<Long> findLikedCommentIdsByUserIdAndCommentIds(@Param("userId") String userId, @Param("commentIds") List<Long> commentIds);

    @Modifying
    @Transactional
    long deleteByComment_IdAndUserId(Long commentId, String userId);
}
