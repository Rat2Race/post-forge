package dev.iamrat.collector.source.infrastructure.external.naver;

import dev.iamrat.collector.item.domain.CollectedItem;
import dev.iamrat.collector.source.application.NewsSearchClient;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsApiClient implements NewsSearchClient {

    private static final String SOURCE_NAME = "naver-news";

    private final RestClient naverNewsRestClient;

    @Override
    public List<CollectedItem> search(String keyword, int display) {
        NaverNewsApiResponse response;
        try {
            response = naverNewsRestClient.get()
                .uri("/v1/search/news.json?query={query}&display={display}&sort=date", keyword, display)
                .retrieve()
                .body(NaverNewsApiResponse.class);
        } catch (Exception e) {
            log.error("[{}] '{}' 키워드 검색 실패", SOURCE_NAME, keyword, e);
            return Collections.emptyList();
        }

        if (response == null || response.items() == null || response.items().isEmpty()) {
            log.debug("[{}] '{}' 키워드 검색 결과 없음", SOURCE_NAME, keyword);
            return Collections.emptyList();
        }

        return response.items().stream()
            .map(item -> new CollectedItem(
                item.title(),
                item.originalLink(),
                item.link(),
                item.description(),
                item.pubDate()
            ))
            .toList();
    }
}
