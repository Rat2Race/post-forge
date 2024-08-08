package com.springweb.study.service;

import com.springweb.study.domain.dto.ArticleResponse;
import com.springweb.study.repository.ArticleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleService {

	private final ArticleRepo articleRepo;

	//Create
	public void createArticle(ArticleResponse articleDto) {
	}
}
