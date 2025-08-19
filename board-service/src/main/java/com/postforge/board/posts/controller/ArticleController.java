package rat.boardservice.posts.controller;

import rat.boardservice.posts.dto.article.ArticleRequest;
import rat.boardservice.posts.dto.article.ArticleResponse;
import rat.boardservice.posts.service.ArticleService;
import rat.boardservice.posts.service.PagingService;
import rat.boardservice.posts.service.ViewsEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ArticleController {

	private final ArticleService articleService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final PagingService pagingService;

	//Create
	@PostMapping("/create")
	public ResponseEntity<ArticleRequest> createArticle(@RequestBody ArticleRequest articleRequest) {
		Long articleId = articleService.createArticle(articleRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(articleRequest);
	}

	//Read
	// /read/article?id=1
	@GetMapping("/read/{id}")
	public ResponseEntity<ArticleResponse> readArticleById(@PathVariable Long id) {
		ArticleResponse articleResponse = articleService.readArticleById(id);

		//eventPublisher
		applicationEventPublisher.publishEvent(new ViewsEvent(articleResponse));

		return ResponseEntity.status(HttpStatus.OK).body(articleResponse);
	}

	@GetMapping("/read")
	public ResponseEntity<Page<ArticleResponse>> readArticles(
			@PageableDefault(size = 10) Pageable pageable) {
		Page<ArticleResponse> paging = this.pagingService.getArticles(pageable);
		return ResponseEntity.status(HttpStatus.OK).body(paging);
	}

	//Update
	// /update?id=1
	@PutMapping("/update")
	public ResponseEntity<ArticleResponse> updateArticle(@RequestBody ArticleRequest articleRequest, @RequestParam Long id) {
		return ResponseEntity.status(HttpStatus.OK).body(articleService.updateArticleById(id, articleRequest));
	}

	//Delete
	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteArticle(@RequestParam Long id) {
		articleService.deleteArticleById(id);
		return ResponseEntity.status(HttpStatus.OK).body("Delete Success");
	}

}