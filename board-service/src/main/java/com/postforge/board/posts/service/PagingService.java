package com.postforge.board.posts.service;

import com.postforge.board.posts.domain.entity.Article;
import com.postforge.board.posts.domain.dto.article.ArticleResponse;
import com.postforge.board.posts.domain.repository.ArticleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagingService {

	private final ArticleRepo articleRepo;

	public Page<ArticleResponse> getArticles(Pageable pageable) {
		Page<Article> articlePage = articleRepo.findAll(pageable);
		return articlePage.map(ArticleResponse::toEntity);
	}

	public Page<ArticleResponse> searchByTitle(String title, Pageable pageable) {
		return articleRepo.findByTitleContaining(title, pageable);
	}
}