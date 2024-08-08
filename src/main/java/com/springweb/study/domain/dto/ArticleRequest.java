package com.springweb.study.domain.dto;

import lombok.Data;

@Data
public class ArticleRequest {
	private String title;
	private String content;
	private String author;
}
