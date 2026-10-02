package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.like.domain.CommentLike;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByComment_IdAndAccountId(Long commentId, Long accountId);

    long countByComment_Id(Long commentId);

    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id IN :commentIds GROUP BY cl.comment.id")
    List<Object[]> countByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.accountId = :accountId AND cl.comment.id IN :commentIds")
    Set<Long> findLikedCommentIdsByAccountIdAndCommentIds(@Param("accountId") Long accountId, @Param("commentIds") List<Long> commentIds);

    // 같은 계정의 요청 두 개가 함께 와도 행은 하나만 생긴다. 충돌은 예외 대신 0을 돌려준다.
    // 예외로 받으면 PostgreSQL이 그 트랜잭션의 다음 문장을 거절하고, Hibernate 세션에도 id 없는 엔티티가 남는다.
    // 네이티브 문장이라 JPA auditing이 돌지 않아 시각과 작성자를 직접 넣는다.
    @Modifying
    @Query(value = """
        INSERT INTO comment_like (comment_id, account_id, created_at, modified_at, created_by, modified_by)
        VALUES (:commentId, :accountId, :now, :now, :auditor, :auditor)
        ON CONFLICT (comment_id, account_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("commentId") Long commentId,
        @Param("accountId") Long accountId,
        @Param("now") LocalDateTime now,
        @Param("auditor") String auditor
    );

    // 엔티티를 읽어 하나씩 지우는 파생 delete는 동시 취소 때 이미 지워진 행을 지우려다 실패한다. 한 문장으로 지우고 지운 수를 돌려준다.
    @Modifying
    @Query("DELETE FROM CommentLike l WHERE l.comment.id = :commentId AND l.accountId = :accountId")
    int deleteByCommentIdAndAccountId(@Param("commentId") Long commentId, @Param("accountId") Long accountId);
}
