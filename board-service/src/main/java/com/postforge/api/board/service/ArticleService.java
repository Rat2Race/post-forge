package com.postforge.api.board.service;

import com.postforge.domain.board.dto.ArticleDto;
import com.postforge.domain.board.entity.Article;
import com.postforge.domain.board.repository.ArticleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

    private final ArticleRepository articleRepository;

    public Long saveArticle(ArticleDto articleDto) {
        Article article = articleDto.toEntity();
        return articleRepository.save(article).getId();
    }

    @Transactional(readOnly = true)
    public List<ArticleDto> getArticles() {
        List<Article> articles = articleRepository.findAll();
        return articles.stream().map(ArticleDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ArticleDto getArticle(Long articleId) {
        return articleRepository.findById(articleId)
            .map(ArticleDto::from)
            .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다."));
    }

    public void updateArticle(Long id, ArticleDto articleDto) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다."));
        article.update(articleDto);
    }

    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }

//    public void countViews(Long id) {
//        Article article = getArticleById(id);
//        Long views = article.getViews();
//        article.updateViews(++views);
//    }
//
//    public Page<ArticleDto> searchArticles(String searchKeyword, Pageable pageable) {
//        if (searchKeyword == null) {
//            return articleRepository.findAll(pageable).map(ArticleDto::from);
//        }
//        return null;
//    }
}