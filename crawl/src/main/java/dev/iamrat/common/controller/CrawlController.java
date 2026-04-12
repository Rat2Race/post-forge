package dev.iamrat.crawl.common.controller;

import dev.iamrat.crawl.common.DataSourceCrawler;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crawl")
@RequiredArgsConstructor
public class CrawlController {

    private final List<DataSourceCrawler> crawlers;

    @PostMapping("/{source}")
    public ResponseEntity<Map<String, String>> trigger(@PathVariable String source) {
        DataSourceCrawler target = crawlers.stream()
                .filter(c -> c.getSourceName().equals(source))
                .findFirst()
                .orElse(null);

        if (target == null) {
            List<String> available = crawlers.stream()
                    .map(DataSourceCrawler::getSourceName)
                    .toList();

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown source",
                    "available", available.toString()
            ));
        }

        target.crawl();
        return ResponseEntity.ok(Map.of("message", source + " 크롤링 완료"));
    }
}
