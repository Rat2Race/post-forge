package dev.iamrat.source.news.infrastructure.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record NaverNewsApiResponse(List<NaverNewsApiItem> items) {

    NaverNewsApiResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record NaverNewsApiItem(
    String title,
    String originallink,
    String link,
    String description,
    String pubDate
) {
}
