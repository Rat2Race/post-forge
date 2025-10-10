package com.postforge.api.board.controller;

import com.postforge.api.board.service.ArticleCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class ArticleCommentController {

    private final ArticleCommentService articleCommentService;

    @PostMapping("/create/{article_id}")
    public ResponseEntity<Long> createComment(@PathVariable Long articleId) {
        return ResponseEntity.status(HttpStatus.OK).body(articleCommentService.saveArticleComment(articleId));
    }
}

