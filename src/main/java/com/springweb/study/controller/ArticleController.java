package com.springweb.study.controller;

import com.springweb.study.domain.dto.ArticleRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class ArticleController {

	//Create
	@PostMapping("/create")
	public ResponseEntity<?> createArticle(@RequestBody ArticleRequest articleRequest) {
		return ResponseEntity.status(HttpStatus.CREATED).body(articleRequest);
	}
}
