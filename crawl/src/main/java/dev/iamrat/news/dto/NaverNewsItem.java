package dev.iamrat.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverNewsItem(
        String title,
        @JsonProperty("originallink") String originalLink,
        String link,
        String description,
        String pubDate
) {
}

