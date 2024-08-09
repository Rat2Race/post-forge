package com.springweb.study.service;

import com.springweb.study.domain.Article;
import com.springweb.study.domain.dto.ArticleRequest;
import com.springweb.study.domain.dto.ArticleResponse;
import com.springweb.study.repository.ArticleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

	private final ArticleRepo articleRepo;

	//Create
	public Long createArticle(ArticleRequest articleRequest) {
		Article article = Article.builder()
				.title(articleRequest.getTitle())
				.content(articleRequest.getContent())
				.author(articleRequest.getAuthor())
				.build();

		return articleRepo.save(article).getId();
	}

	//Read
	@Transactional(readOnly = true)
	public List<ArticleResponse> readArticle() {
		return articleRepo.findAll()
				.stream()
				.map(ArticleResponse::toEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public ArticleResponse readArticleById(Long id) {
		return ArticleResponse.toEntity(articleRepo.findById(id).orElseThrow(
				() -> new IllegalArgumentException("article doesn't exit")
		));
	}

	//Update
	public ArticleResponse updateArticleById(Long id, ArticleRequest articleRequest) {
		Article article = getArticleById(id);
		article.update(articleRequest);

		return ArticleResponse.toEntity(article);
	}

	//Delete
	public void deleteArticleById(Long id) {
		Article article = getArticleById(id);
		articleRepo.deleteById(article.getId());
	}

	private Article getArticleById(Long id) {
		return articleRepo.findById(id).orElseThrow(
				() -> new IllegalArgumentException("article doesn't exit")
		);
	}

}
