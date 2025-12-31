package dev.iamrat.comment.repository;

import dev.iamrat.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.userId = :userId")
    boolean existsByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.userId = :userId")
    void deleteByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);
}
