package com.postforge.board.posts.domain.dto.article;

import com.postforge.board.posts.domain.entity.Article;

public record ArticleResponse (
	Long id,
	String title,
	String content,
	String author,
	Long views
) {
	public static ArticleResponse toEntity(Article article) {
		return new ArticleResponse (
				article.getId(),
				article.getTitle(),
				article.getContent(),
				article.getAuthor(),
				article.getViews()
		);
	}
}