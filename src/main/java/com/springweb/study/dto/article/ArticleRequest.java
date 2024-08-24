package com.springweb.study.dto.article;

import lombok.Data;

@Data
public class ArticleRequest {
	private String title;
	private String content;
	private String author;
}
