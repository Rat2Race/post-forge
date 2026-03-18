package dev.iamrat.crawl.dto;

import java.util.List;

public record NaverNewsApiResponse(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NaverNewsItem> items
) {
}
