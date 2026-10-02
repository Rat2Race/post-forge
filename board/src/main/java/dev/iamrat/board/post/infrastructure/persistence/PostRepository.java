package dev.iamrat.board.post.infrastructure.persistence;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostType;
import dev.iamrat.core.board.post.NewsSection;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Query("""
        SELECT p FROM Post p
        WHERE p.category = :category
          AND p.boardCategory = :boardCategory
          AND p.createdAt >= :startInclusive
          AND p.createdAt < :endExclusive
        """)
    List<Post> findAllByCategoryAndBoardCategoryInRange(
        @Param("category") PostType category,
        @Param("boardCategory") NewsSection boardCategory,
        @Param("startInclusive") LocalDateTime startInclusive,
        @Param("endExclusive") LocalDateTime endExclusive
    );

    boolean existsByCategoryAndBoardCategoryAndTitle(PostType category, NewsSection boardCategory, String title);

    @Modifying
    @Query("UPDATE Post p SET p.views = :views WHERE p.id = :id")
    void updateViews(@Param("id") Long id, @Param("views") long views);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = :likeCount WHERE p.id = :id")
    void updateLikeCount(@Param("id") Long id, @Param("likeCount") long likeCount);
}
