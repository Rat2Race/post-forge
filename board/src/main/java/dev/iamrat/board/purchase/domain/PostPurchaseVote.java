package dev.iamrat.board.purchase.domain;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.support.persistence.AuditingFields;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
    name = "post_purchase_vote",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_post_purchase_vote_post_account",
        columnNames = {"post_id", "account_id"}
    ),
    indexes = {
        @Index(name = "idx_post_purchase_vote_post_id", columnList = "post_id"),
        @Index(name = "idx_post_purchase_vote_account_post", columnList = "account_id, post_id")
    }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPurchaseVote extends AuditingFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 30)
    private PurchaseVoteType voteType;

    public static PostPurchaseVote of(Post post, Long accountId, PurchaseVoteType voteType) {
        return PostPurchaseVote.builder()
            .post(post)
            .accountId(accountId)
            .voteType(voteType)
            .build();
    }

    public void updateVote(PurchaseVoteType voteType) {
        this.voteType = voteType;
    }
}
