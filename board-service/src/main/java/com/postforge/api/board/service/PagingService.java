//package com.postforge.api.board.service;
//
//import com.postforge.domain.article.entity.Article;
//import com.postforge.domain.article.dto.ArticleResponse;
//import com.postforge.domain.article.repository.ArticleRepo;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.*;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class PagingService {
//
//	private final ArticleRepo articleRepo;
//
//	public Page<ArticleResponse> getArticles(Pageable pageable) {
//		Page<Article> articlePage = articleRepo.findAll(pageable);
//		return articlePage.map(ArticleResponse::toEntity);
//	}
//
//	public Page<ArticleResponse> searchByTitle(String title, Pageable pageable) {
//		return articleRepo.findByTitleContaining(title, pageable);
//	}
//}