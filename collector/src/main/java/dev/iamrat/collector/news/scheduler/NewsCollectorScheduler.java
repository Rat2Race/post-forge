package dev.iamrat.collector.news.scheduler;

import dev.iamrat.collector.collection.service.DataSourceCollector;
import dev.iamrat.collector.news.service.NaverNewsCollectorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectorScheduler.class);

    private final NaverNewsCollectorService naverNewsCollectorService;

    @Scheduled(cron = "${collector.naver-news.cron:0 0 */2 * * *}")
    public void collectNaverNews() {
        runCollector(naverNewsCollectorService);
    }

    private void runCollector(DataSourceCollector collector) {
        try {
            log.info("[{}] 수집 시작", collector.getSourceName());
            collector.collect();
        } catch (Exception e) {
            log.error("[{}] 수집 실패", collector.getSourceName(), e);
        }
    }
}
