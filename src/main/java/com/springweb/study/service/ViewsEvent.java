package com.springweb.study.service;

import com.springweb.study.domain.Article;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ViewsEvent extends ApplicationEvent {

	private final Article article;

	public ViewsEvent(Article article) {
		super(article);
		this.article = article;
	}
}
