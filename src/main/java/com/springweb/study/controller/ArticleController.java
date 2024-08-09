package com.springweb.study.controller;

import com.springweb.study.domain.dto.ArticleRequest;
import com.springweb.study.domain.dto.ArticleResponse;
import com.springweb.study.service.ArticleService;
import com.sun.net.httpserver.Authenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class ArticleController {

	private final ArticleService articleService;

	//Create
	@PostMapping("/create")
	public ResponseEntity<?> createArticle(@RequestBody ArticleRequest articleRequest) {
		Long articleId = articleService.createArticle(articleRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(articleRequest);
	}

	//Read
	@GetMapping("/read/{id}")
	public ResponseEntity<ArticleResponse> readArticle(@PathVariable Long id) {
		articleService.readArticle();
	}
}
