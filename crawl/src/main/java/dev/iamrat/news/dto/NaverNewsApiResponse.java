package dev.iamrat.crawl.news.dto;

import java.util.List;

public record NaverNewsApiResponse(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NaverNewsItem> items
) {
}
