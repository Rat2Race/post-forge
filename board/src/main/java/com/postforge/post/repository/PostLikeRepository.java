package com.postforge.post.repository;

import com.postforge.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN true ELSE false END FROM PostLike pl WHERE pl.post.id = :postId AND pl.userId = :userId")
    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId AND pl.userId = :userId")
    void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
