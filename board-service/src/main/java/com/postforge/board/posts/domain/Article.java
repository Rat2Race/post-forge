package com.postforge.board.posts.domain;

import com.postforge.board.posts.dto.article.ArticleRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "article")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Article extends AuditingFields {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title")
	private String title;

	@Column(name = "content")
	private String content;

	@Column(name = "author")
	private String author;

	@Column(name = "views")
	private Long views;

	public void update(ArticleRequest articleRequest) {
		this.title = articleRequest.title();
		this.content = articleRequest.content();
		this.author = articleRequest.author();
	}

	public void updateViews(Long count) {
		this.views = count;
	}
}