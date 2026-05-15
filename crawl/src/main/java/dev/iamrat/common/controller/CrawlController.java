package dev.iamrat.common.controller;

import dev.iamrat.common.DataSourceCrawler;
import dev.iamrat.common.dto.CrawlErrorResponse;
import dev.iamrat.common.dto.CrawlResponse;
import dev.iamrat.common.dto.CrawlTriggerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/crawl")
@RequiredArgsConstructor
public class CrawlController {

    private final List<DataSourceCrawler> crawlers;

    @PostMapping("/{source}")
    public ResponseEntity<CrawlResponse> trigger(@PathVariable String source) {
        DataSourceCrawler target = crawlers.stream()
                .filter(c -> c.getSourceName().equals(source))
                .findFirst()
                .orElse(null);

        if (target == null) {
            List<String> available = crawlers.stream()
                    .map(DataSourceCrawler::getSourceName)
                    .toList();

            return ResponseEntity
                    .badRequest()
                    .body(new CrawlErrorResponse("Unknown source", available.toString()));
        }

        target.crawl();
        return ResponseEntity.ok(CrawlTriggerResponse.of(source + " 크롤링 완료"));
    }
}
