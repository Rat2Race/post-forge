package dev.iamrat.board.post.infrastructure.persistence;

import dev.iamrat.board.post.domain.PostProductLink;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostProductLinkRepository extends JpaRepository<PostProductLink, Long> {
    @EntityGraph(attributePaths = "post")
    List<PostProductLink> findByProductIdOrderByCreatedAtDesc(Long productId);
}
