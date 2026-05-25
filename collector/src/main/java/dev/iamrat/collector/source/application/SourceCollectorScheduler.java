package dev.iamrat.collector.source.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SourceCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(SourceCollectorScheduler.class);

    private final DataSourceCollector naverNewsCollector;

    public SourceCollectorScheduler(@Qualifier("naverNewsCollectorService") DataSourceCollector naverNewsCollector) {
        this.naverNewsCollector = naverNewsCollector;
    }

    @Scheduled(cron = "${collector.naver-news.cron:0 0 */2 * * *}")
    public void collectNaverNews() {
        runCollector(naverNewsCollector);
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
