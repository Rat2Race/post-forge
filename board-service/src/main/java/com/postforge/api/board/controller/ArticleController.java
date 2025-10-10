package com.postforge.api.board.controller;

import com.postforge.domain.board.dto.ArticleDto;
import com.postforge.api.board.service.ArticleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/articles")
@CrossOrigin(origins = "http://localhost:3000")
public class ArticleController {

    private final ArticleService articleService;
//    private final ApplicationEventPublisher applicationEventPublisher;
//    private final PagingService pagingService;

    @GetMapping("/read")
    public ResponseEntity<List<ArticleDto>> readArticles() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(articleService.getArticles());
    }

    @PostMapping("/create")
    public ResponseEntity<Long> createArticle(@RequestBody ArticleDto articleDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(articleService.saveArticle(articleDto));
    }

    @GetMapping("/read/{id}")
    public ResponseEntity<ArticleDto> readArticleById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(articleService.getArticle(id));
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateArticle(@RequestParam Long id,
        @RequestBody ArticleDto articleDto) {
        articleService.updateArticle(id, articleDto);
        return ResponseEntity.status(HttpStatus.OK).body("게시글 업데이트 완료");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteArticle(@RequestParam Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.status(HttpStatus.OK).body("게시글 삭제 완료");
    }
}