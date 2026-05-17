package dev.iamrat.board.like.comment.entity;

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
import dev.iamrat.board.comment.entity.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "comment_like",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_comment_like_comment_user",
        columnNames = {"comment_id", "user_id"}
    ),
    indexes = @Index(name = "idx_comment_like_user_comment", columnList = "user_id, comment_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class CommentLike extends AuditingFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    public static CommentLike of(Comment comment, String userId) {
        return CommentLike.builder()
            .comment(comment)
            .userId(userId)
            .build();
    }

}
