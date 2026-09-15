package dev.iamrat.board.file.domain;

import dev.iamrat.board.post.domain.Post;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "post_file",
    indexes = @Index(name = "idx_post_file_post_created_at", columnList = "post_id, created_at")
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PostFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String savedFileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    private Long fileSize;

    @Column(nullable = false, length = 100)
    private String fileType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public void assignPost(Post post) {
        this.post = post;
    }

    public void unassignPost() {
        this.post = null;
    }
}
