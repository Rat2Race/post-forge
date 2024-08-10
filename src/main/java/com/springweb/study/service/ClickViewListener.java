package com.springweb.study.service;

import com.springweb.study.domain.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClickViewListener implements ApplicationListener<ViewsEvent> {

	private final ArticleService articleService;

	@Override
	public void onApplicationEvent(ViewsEvent event) {
		Article article = event.getArticle();
		articleService.countViews(article.getId());
	}
}
