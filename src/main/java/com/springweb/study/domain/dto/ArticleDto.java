package com.springweb.study.domain.dto;

import lombok.Data;

@Data
public class ArticleDto {
	private Long id;
	private String title;
	private String content;
	private String author;
}
