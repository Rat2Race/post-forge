package rat.boardservice.posts.service;

import rat.boardservice.posts.domain.Article;
import rat.boardservice.posts.dto.article.ArticleResponse;
import rat.boardservice.posts.repository.ArticleRepo;
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