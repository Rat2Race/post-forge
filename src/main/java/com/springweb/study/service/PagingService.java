package com.springweb.study.service;

import com.springweb.study.domain.Article;
import com.springweb.study.domain.dto.ArticleResponse;
import com.springweb.study.repository.ArticleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagingService {

	private final ArticleRepo articleRepo;

	public Page<ArticleResponse> getList(int page) {
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createDate"));
		Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
		return articleRepo.findAllByPageable(pageable);
	}

	public Page<ArticleResponse> searchByTitle(String title, Pageable pageable) {
		return articleRepo.findByTitleContaining(title, pageable);
	}
}
