package com.springweb.study.controller;

import com.springweb.study.domain.dto.ArticleRequest;
import com.springweb.study.domain.dto.ArticleResponse;
import com.springweb.study.service.ArticleService;
import com.springweb.study.service.ViewsEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ArticleController {

	private final ArticleService articleService;
	private final ApplicationEventPublisher applicationEventPublisher;

	//Create
	@PostMapping("/create")
	public ResponseEntity<ArticleRequest> createArticle(@RequestBody ArticleRequest articleRequest) {
		Long articleId = articleService.createArticle(articleRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(articleRequest);
	}

	//Read
	// /read/article?id=1
	@GetMapping("/read/article/{id}")
	public ResponseEntity<ArticleResponse> readArticleById(@PathVariable Long id) {
		ArticleResponse articleResponse = articleService.readArticleById(id);
		//eventPublisher
		applicationEventPublisher.publishEvent(new ViewsEvent(articleResponse));

		return ResponseEntity.status(HttpStatus.OK).body(articleResponse);
	}

	@GetMapping("/read/article")
	public ResponseEntity<List<ArticleResponse>> readArticles() {
		return ResponseEntity.status(HttpStatus.OK).body(articleService.readArticle());
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
