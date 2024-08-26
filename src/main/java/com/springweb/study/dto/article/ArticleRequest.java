package com.springweb.study.dto.article;

public record ArticleRequest (
		String title,
		String content,
		String author
) {
}
