package dev.iamrat.like.post.repository;

import dev.iamrat.like.post.entity.PostLike;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPost_IdAndUserId(Long postId, String userId);

    long countByPost_Id(Long postId);

    @Query("SELECT pl.post.id, COUNT(pl) FROM PostLike pl WHERE pl.post.id IN :postIds GROUP BY pl.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.userId = :userId AND pl.post.id IN :postIds")
    Set<Long> findLikedPostIdsByUserIdAndPostIds(@Param("userId") String userId, @Param("postIds") List<Long> postIds);

    @Modifying
    @Transactional
    long deleteByPost_IdAndUserId(Long postId, String userId);
}
