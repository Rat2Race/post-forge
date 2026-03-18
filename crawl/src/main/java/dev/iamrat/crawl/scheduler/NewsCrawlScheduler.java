package dev.iamrat.crawl.scheduler;

import dev.iamrat.crawl.common.DataSourceCrawler;
import dev.iamrat.crawl.service.DartCrawlService;
import dev.iamrat.crawl.service.NaverNewsCrawlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCrawlScheduler {

    private final NaverNewsCrawlService naverNewsCrawlService;
    private final DartCrawlService dartCrawlService;

    @Scheduled(cron = "${crawl.naver-news.cron:0 0 */2 * * *}")
    public void crawlNaverNews() {
        runCrawler(naverNewsCrawlService);
    }

    @Scheduled(cron = "${crawl.dart.cron:0 30 */2 * * *}")
    public void crawlDart() {
        runCrawler(dartCrawlService);
    }

    private void runCrawler(DataSourceCrawler crawler) {
        try {
            log.info("[{}] 크롤링 시작", crawler.getSourceName());
            crawler.crawl();
        } catch (Exception e) {
            log.error("[{}] 크롤링 실패", crawler.getSourceName(), e);
        }
    }
}
