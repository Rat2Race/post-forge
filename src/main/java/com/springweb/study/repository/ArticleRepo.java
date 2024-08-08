package com.springweb.study.repository;

import com.springweb.study.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepo extends JpaRepository<Article, Long> {
	Boolean saveArticle(Article article);

}
