package com.springweb.study.domain;

import com.springweb.study.domain.dto.ArticleRequest;
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
		this.title = articleRequest.getTitle();
		this.content = articleRequest.getContent();
		this.author = articleRequest.getAuthor();
	}

	public void updateViews(Long count) {
		this.views = count;
	}
}
