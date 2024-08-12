package com.springweb.study.repository;

import com.springweb.study.domain.Article;
import com.springweb.study.domain.dto.ArticleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepo extends JpaRepository<Article, Long> {
	Page<ArticleResponse> findByTitleContaining(String title, Pageable pageable);
	Page<ArticleResponse> findAllByPageable(Pageable pageable);
}
