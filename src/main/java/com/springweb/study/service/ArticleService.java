package com.springweb.study.service;

import com.springweb.study.domain.Article;
import com.springweb.study.domain.dto.ArticleRequest;
import com.springweb.study.domain.dto.ArticleResponse;
import com.springweb.study.repository.ArticleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {

	private final ArticleRepo articleRepo;

	//Create
	public Long createArticle(ArticleRequest articleRequest) {
		Article article = Article.builder()
				.title(articleRequest.getTitle())
				.content(articleRequest.getContent())
				.author(articleRequest.getAuthor())
				.build();

		return articleRepo.saveArticle(article);
	}

	//Read
	public List<ArticleResponse> readArticle() {
		return articleRepo.findAll()
				.stream()
				.map(ArticleResponse::toEntity)
				.toList();
	}

	//Update
	public ArticleResponse updateArticleById(Long id, ArticleRequest articleRequest) {
		Article article = articleRepo.findById(id).orElseThrow(
				() -> new IllegalArgumentException("article doesn't exit")
		);

		article.update(articleRequest);

		return ArticleResponse.toEntity(article);
	}

	//Delete
	public void deleteArticleById(Long id) {
		Article article = articleRepo.findById(id).orElseThrow(
				() -> new IllegalArgumentException("article doesn't exit")
		);

		articleRepo.deleteById(article.getId());
	}

}
