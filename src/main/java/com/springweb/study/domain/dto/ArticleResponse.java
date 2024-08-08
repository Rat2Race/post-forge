package com.springweb.study.domain.dto;

import lombok.Data;

@Data
public class ArticleResponse {
	private Long id;
	private String title;
	private String content;
	private String author;
}
