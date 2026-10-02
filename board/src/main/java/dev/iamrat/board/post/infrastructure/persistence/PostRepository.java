package dev.iamrat.board.post.infrastructure.persistence;

import dev.iamrat.board.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Modifying
    @Query("UPDATE Post p SET p.views = :views WHERE p.id = :id")
    void updateViews(@Param("id") Long id, @Param("views") long views);

    // 읽은 값을 다시 쓰지 않고 한 문장으로 더한다. 동시 요청은 행 잠금 순서대로 서로의 결과 위에 더한다.
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + :delta WHERE p.id = :id")
    void addLikeCount(@Param("id") Long id, @Param("delta") long delta);
}
