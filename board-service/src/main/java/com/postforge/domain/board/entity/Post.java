package com.postforge.domain.board.entity;

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

	@Column(name = "views")
	@Builder.Default
	private Long views = 0L;

	@Column(name = "user_id", nullable = false, updatable = false, length = 100)
	private String userId;

	@ToString.Exclude
	@OrderBy("createdAt DESC")
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
	@Builder.Default
	private Set<Comment> comments = new LinkedHashSet<>();

	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public void updateViews(Long count) {
		this.views = count;
	}
}
