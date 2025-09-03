package com.postforge.board.posts.service;

import com.postforge.board.posts.domain.entity.Article;
import com.postforge.board.posts.domain.dto.article.ArticleRequest;
import com.postforge.board.posts.domain.dto.article.ArticleResponse;
import com.postforge.board.posts.domain.repository.ArticleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

	private final ArticleRepo articleRepo;

	//Create
	public Long createArticle(ArticleRequest articleRequest) {
		Article article = Article.builder()
				.title(articleRequest.title())
				.content(articleRequest.content())
				.author(articleRequest.author())
				.views(0L)
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

	//조회 카운팅
	public void countViews(Long id) {
		Article article = getArticleById(id);
		Long views = article.getViews();
		article.updateViews(++views);
	}
}