package com.springweb.study.service;

import com.springweb.study.domain.Article;
import com.springweb.study.domain.dto.ArticleResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ViewsEvent extends ApplicationEvent {

	private final Article article;

	public ViewsEvent(ArticleResponse articleResponse) {
		super(articleResponse);
		this.article = Article.builder()
				.title(articleResponse.getTitle())
				.content(articleResponse.getContent())
				.author(articleResponse.getAuthor())
				.views(articleResponse.getViews())
				.build();
	}
}
