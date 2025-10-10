package com.postforge.domain.board.entity;

import com.postforge.domain.board.dto.ArticleDto;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "article")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Article extends AuditingFields {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, length = 10000)
	private String content;

	@Column(name = "author")
	private String author;

	@Column(name = "views")
	private Long views;

	@ToString.Exclude
	@OrderBy("createdAt DESC")
	@OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
	private final Set<ArticleComment> articleComments = new LinkedHashSet<>();

	public void update(ArticleDto articleDto) {
		this.title = articleDto.title();
		this.content = articleDto.content();
	}

	public void updateViews(Long count) {
		this.views = count;
	}
}