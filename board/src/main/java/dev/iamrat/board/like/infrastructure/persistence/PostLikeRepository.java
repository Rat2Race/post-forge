package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.like.domain.PostLike;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPost_IdAndAccountId(Long postId, Long accountId);

    long countByPost_Id(Long postId);

    @Query("SELECT pl.post.id, COUNT(pl) FROM PostLike pl WHERE pl.post.id IN :postIds GROUP BY pl.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.accountId = :accountId AND pl.post.id IN :postIds")
    Set<Long> findLikedPostIdsByAccountIdAndPostIds(@Param("accountId") Long accountId, @Param("postIds") List<Long> postIds);

    // 같은 계정의 요청 두 개가 함께 와도 행은 하나만 생긴다. 충돌은 예외 대신 0을 돌려준다.
    // 예외로 받으면 PostgreSQL이 그 트랜잭션의 다음 문장을 거절하고, Hibernate 세션에도 id 없는 엔티티가 남는다.
    // 네이티브 문장이라 JPA auditing이 돌지 않아 시각과 작성자를 직접 넣는다.
    @Modifying
    @Query(value = """
        INSERT INTO post_like (post_id, account_id, created_at, modified_at, created_by, modified_by)
        VALUES (:postId, :accountId, :now, :now, :auditor, :auditor)
        ON CONFLICT (post_id, account_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("postId") Long postId,
        @Param("accountId") Long accountId,
        @Param("now") LocalDateTime now,
        @Param("auditor") String auditor
    );

    // 엔티티를 읽어 하나씩 지우는 파생 delete는 동시 취소 때 이미 지워진 행을 지우려다 실패한다. 한 문장으로 지우고 지운 수를 돌려준다.
    @Modifying
    @Query("DELETE FROM PostLike l WHERE l.post.id = :postId AND l.accountId = :accountId")
    int deleteByPostIdAndAccountId(@Param("postId") Long postId, @Param("accountId") Long accountId);
}
