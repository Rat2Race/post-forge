package dev.iamrat.board.like.post.entity;

import dev.iamrat.board.support.persistence.AuditingFields;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import dev.iamrat.board.post.entity.Post;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "post_like",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_post_like_post_user",
        columnNames = {"post_id", "user_id"}
    ),
    indexes = @Index(name = "idx_post_like_user_post", columnList = "user_id, post_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class PostLike extends AuditingFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    public static PostLike of(Post post, String userId) {
        return PostLike.builder()
            .post(post)
            .userId(userId)
            .build();
    }

}
