package com.springweb.study.service;

import com.springweb.study.domain.dto.ArticleResponse;
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
