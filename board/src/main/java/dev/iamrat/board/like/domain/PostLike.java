package dev.iamrat.board.like.domain;

import dev.iamrat.board.post.domain.Post;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "post_like",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_post_like_post_account",
        columnNames = {"post_id", "account_id"}
    ),
    indexes = @Index(name = "idx_post_like_account_post", columnList = "account_id, post_id")
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

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    public static PostLike of(Post post, Long accountId) {
        return PostLike.builder()
            .post(post)
            .accountId(accountId)
            .build();
    }

}
