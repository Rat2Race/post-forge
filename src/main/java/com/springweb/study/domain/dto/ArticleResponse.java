package com.springweb.study.domain.dto;

import com.springweb.study.domain.Article;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleResponse {
	private Long id;
	private String title;
	private String content;
	private String author;

	public static ArticleResponse toEntity(Article article) {
		return ArticleResponse.builder()
				.id(article.getId())
				.title(article.getTitle())
				.content(article.getContent())
				.author(article.getAuthor())
				.build();
	}
}
