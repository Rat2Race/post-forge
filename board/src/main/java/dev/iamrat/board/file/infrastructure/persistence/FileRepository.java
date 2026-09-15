package dev.iamrat.board.file.infrastructure.persistence;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<PostFile, Long> {

    List<PostFile> findAllByIdIn(List<Long> ids);

    List<PostFile> findAllByPost(Post post);

    @Modifying
    @Query("DELETE FROM PostFile f WHERE f.post IS NULL AND f.createdAt < :threshold")
    int deleteOrphanFiles(@Param("threshold") LocalDateTime threshold);
}
