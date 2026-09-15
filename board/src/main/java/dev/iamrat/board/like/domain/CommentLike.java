package dev.iamrat.board.like.domain;

import dev.iamrat.board.comment.domain.Comment;
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
    name = "comment_like",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_comment_like_comment_account",
        columnNames = {"comment_id", "account_id"}
    ),
    indexes = @Index(name = "idx_comment_like_account_comment", columnList = "account_id, comment_id")
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

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    public static CommentLike of(Comment comment, Long accountId) {
        return CommentLike.builder()
            .comment(comment)
            .accountId(accountId)
            .build();
    }

}
