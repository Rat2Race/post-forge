package dev.iamrat.board.post.domain;

import dev.iamrat.board.support.persistence.AuditingFields;
import dev.iamrat.board.comment.domain.Comment;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
	name = "posts",
	indexes = {
		@Index(name = "idx_posts_created_at", columnList = "created_at"),
		@Index(name = "idx_posts_account_id", columnList = "account_id")
	}
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post extends AuditingFields {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 10000)
	private String content;

	@ElementCollection
	@CollectionTable(
		name = "post_tags",
		joinColumns = @JoinColumn(name = "post_id"),
		indexes = @Index(name = "idx_post_tags_post_id", columnList = "post_id")
	)
	@Column(name = "tag", length = 50)
	@Builder.Default
	private List<String> tags = new ArrayList<>();

	@Column(name = "views", nullable = false)
	@Builder.Default
	private Long views = 0L;

	@Column(name = "like_count", nullable = false)
	@Builder.Default
	private Long likeCount = 0L;

	@Column(name = "account_id", nullable = false, updatable = false)
	private Long accountId;

	@Column(name = "nickname", nullable = false, length = 50)
	private String nickname;

	@ToString.Exclude
	@OrderBy("createdAt DESC")
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<Comment> comments = new LinkedHashSet<>();

	public static Post general(String title, String content, Long accountId, String nickname) {
		return create(title, content, null, accountId, nickname);
	}

	public static Post create(String title, String content, List<String> tags, Long accountId, String nickname) {
		return Post.builder()
			.title(title)
			.content(content)
			.tags(tags == null ? new ArrayList<>() : new ArrayList<>(tags))
			.accountId(accountId)
			.nickname(nickname)
			.build();
	}

	public void addComment(Comment comment) {
		this.comments.add(comment);
	}

	public void update(String title, String content, List<String> tags) {
		this.title = title;
		this.content = content;
		this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
	}

	public void updateViews(Long count) {
		this.views = count;
	}

}
