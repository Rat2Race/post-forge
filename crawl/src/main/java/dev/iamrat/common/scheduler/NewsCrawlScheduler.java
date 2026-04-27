package dev.iamrat.common.scheduler;

import dev.iamrat.common.DataSourceCrawler;
import dev.iamrat.news.service.NaverNewsCrawlService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsCrawlScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlScheduler.class);

    private final NaverNewsCrawlService naverNewsCrawlService;

    @Scheduled(cron = "${crawl.naver-news.cron:0 0 */2 * * *}")
    public void crawlNaverNews() {
        runCrawler(naverNewsCrawlService);
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
