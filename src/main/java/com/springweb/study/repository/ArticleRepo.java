package com.springweb.study.repository;

import com.springweb.study.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepo extends JpaRepository<Article, Long> {
	Page<Article> findByTitleContaining(String title, Pageable pageable);
}
