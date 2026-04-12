package dev.iamrat.crawl.common.scheduler;

import dev.iamrat.crawl.common.DataSourceCrawler;
import dev.iamrat.crawl.dart.service.DartCrawlService;
import dev.iamrat.crawl.news.service.NaverNewsCrawlService;
import dev.iamrat.crawl.price.service.KrxPriceCrawlService;
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
    private final DartCrawlService dartCrawlService;
    private final KrxPriceCrawlService krxPriceCrawlService;

    @Scheduled(cron = "${crawl.naver-news.cron:0 0 */2 * * *}")
    public void crawlNaverNews() {
        runCrawler(naverNewsCrawlService);
    }

    @Scheduled(cron = "${crawl.dart.cron:0 30 */2 * * *}")
    public void crawlDart() {
        runCrawler(dartCrawlService);
    }

    @Scheduled(cron = "${crawl.krx.cron:0 0 18 * * MON-FRI}")
    public void crawlKrxPrices() {
        runCrawler(krxPriceCrawlService);
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
