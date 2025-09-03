package com.postforge.board.posts.domain.dto.article;

public record ArticleRequest (
		String title,
		String content,
		String author
) {
}