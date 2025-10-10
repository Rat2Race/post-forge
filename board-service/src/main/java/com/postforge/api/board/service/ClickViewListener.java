//package com.postforge.api.board.service;
//
//import com.postforge.domain.article.dto.ArticleResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.ApplicationListener;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class ClickViewListener implements ApplicationListener<ViewsEvent> {
//
//	private final ArticleService articleService;
//
//	@Override
//	public void onApplicationEvent(ViewsEvent event) {
//		ArticleResponse articleResponse = event.getArticleResponse();
//		articleService.countViews(articleResponse.id());
//	}
//}