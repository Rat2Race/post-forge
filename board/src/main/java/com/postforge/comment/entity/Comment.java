package com.postforge.comment.entity;

import com.postforge.common.entity.AuditingFields;
import com.postforge.post.entity.Post;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "comments")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends AuditingFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "user_id", nullable = false, updatable = false, length = 100)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    public void updateContent(String newContent) {
        this.content = newContent;
    }

    public void addReply(Comment comment) {
        this.replies.add(comment);
        comment.parent = this;
    }

    public void removeReply(Comment comment) {
        this.replies.remove(comment);
    }

    public void incrementLikeCount() {
        if (this.likeCount == null) {
            this.likeCount = 0L;
        }
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount == null) {
            this.likeCount = 0L;
        }
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public Long getLikeCount() {
        return this.likeCount == null ? 0L : this.likeCount;
    }
}
