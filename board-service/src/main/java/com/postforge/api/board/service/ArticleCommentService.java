package com.postforge.api.board.service;

import com.postforge.domain.board.entity.Article;
import com.postforge.domain.board.entity.ArticleComment;
import com.postforge.domain.board.repository.ArticleCommentRepository;
import com.postforge.domain.board.repository.ArticleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleCommentService {

    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;

    public Long saveArticleComment(Long articleId) {
        Article foundArticle = articleRepository.findById(articleId)
            .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다."));

        ArticleComment newComment = ArticleComment.builder()
            .article(foundArticle)
            .author("TEST_USER")
            .build();

        articleCommentRepository.save(newComment);
        return newComment.getId();
    }

}
