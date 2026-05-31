package dev.iamrat.board.post.infrastructure.persistence;

import dev.iamrat.board.post.domain.PostProductLink;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostProductLinkRepository extends JpaRepository<PostProductLink, Long> {
    boolean existsByProductIdAndPostDate(Long productId, LocalDate postDate);

    List<PostProductLink> findByProductIdOrderByCreatedAtDesc(Long productId);

    Page<PostProductLink> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
