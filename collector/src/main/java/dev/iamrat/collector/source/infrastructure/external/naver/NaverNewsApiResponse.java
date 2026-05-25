package dev.iamrat.collector.source.infrastructure.external.naver;

import java.util.List;

public record NaverNewsApiResponse(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NaverNewsItem> items
) {
}

