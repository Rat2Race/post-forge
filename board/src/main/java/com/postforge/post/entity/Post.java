package com.postforge.post.entity;

import com.postforge.common.entity.AuditingFields;
import com.postforge.comment.entity.Comment;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "posts")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post extends AuditingFields {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, length = 10000)
	private String content;

	@Column(name = "views", nullable = false)
	@Builder.Default
	private Long views = 0L;

	@Column(name = "like_count", nullable = false)
	@Builder.Default
	private Long likeCount = 0L;

	@Column(name = "user_id", nullable = false, updatable = false, length = 100)
	private String userId;

	@ToString.Exclude
	@OrderBy("createdAt DESC")
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<Comment> comments = new LinkedHashSet<>();

	public void addComment(Comment comment) {
		this.comments.add(comment);
	}

	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public void updateViews(Long count) {
		this.views = count;
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

	public Long getViews() {
		return this.views == null ? 0L : this.views;
	}

	public Long getLikeCount() {
		return this.likeCount == null ? 0L : this.likeCount;
	}
}
