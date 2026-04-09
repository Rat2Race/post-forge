package dev.iamrat.post.entity;

import dev.iamrat.common.entity.AuditingFields;
import dev.iamrat.comment.entity.Comment;
import dev.iamrat.file.entity.PostFile;
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

	@Column(length = 500)
	private String summary;

	@ElementCollection
	@CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
	@Column(name = "tag", length = 50)
	@Builder.Default
	private List<String> tags = new ArrayList<>();

	@Column(name = "views", nullable = false)
	@Builder.Default
	private Long views = 0L;

	@Column(name = "like_count", nullable = false)
	@Builder.Default
	private Long likeCount = 0L;

	@Column(name = "user_id", nullable = false, updatable = false, length = 100)
	private String userId;

	@Column(name = "nickname", length = 50)
	private String nickname;

	@ToString.Exclude
	@OrderBy("createdAt DESC")
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<Comment> comments = new LinkedHashSet<>();

	@ToString.Exclude
	@OneToMany(mappedBy = "post")
	@Builder.Default
	private Set<PostFile> files = new LinkedHashSet<>();

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

}
