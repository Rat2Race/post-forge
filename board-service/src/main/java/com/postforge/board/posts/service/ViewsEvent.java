package com.postforge.board.posts.service;

import com.postforge.board.posts.dto.article.ArticleResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ViewsEvent extends ApplicationEvent {

	private final ArticleResponse articleResponse;

	public ViewsEvent(ArticleResponse articleResponse) {
		super(articleResponse);
		this.articleResponse = articleResponse;
	}
}