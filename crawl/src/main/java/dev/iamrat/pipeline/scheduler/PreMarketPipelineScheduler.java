package dev.iamrat.pipeline.scheduler;

import dev.iamrat.candidate.entity.CandidateSelection;
import dev.iamrat.candidate.service.CandidateSelector;
import dev.iamrat.pipeline.config.PipelineConfig;
import dev.iamrat.pipeline.service.CandidatePostPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PreMarketPipelineScheduler {

    private static final Logger log = LoggerFactory.getLogger(PreMarketPipelineScheduler.class);

    private final PipelineConfig pipelineConfig;
    private final CandidateSelector candidateSelector;
    private final CandidatePostPublisher candidatePostPublisher;
    private final Clock clock;

    @Scheduled(cron = "${crawl.pipeline.candidate-cron:0 0 7 * * MON-FRI}")
    public void selectCandidates() {
        if (!pipelineConfig.isEnabled() || pipelineConfig.isKillSwitch()) {
            log.warn("[pipeline] candidate selection skipped - pipeline disabled or kill switch on");
            return;
        }
        List<CandidateSelection> selections = candidateSelector.select(LocalDate.now(clock));
        log.info("[pipeline] candidate selection completed - {} candidates", selections.size());
    }

    @Scheduled(cron = "${crawl.pipeline.publish-cron:0 30 8 * * MON-FRI}")
    public void generatePosts() {
        if (!pipelineConfig.isEnabled() || pipelineConfig.isKillSwitch()) {
            log.warn("[pipeline] publish skipped - pipeline disabled or kill switch on");
            return;
        }

        int cap = Math.max(0, pipelineConfig.getDailyCap());
        List<CandidateSelection> selections = candidateSelector.getSelections(LocalDate.now(clock));
        int count = candidatePostPublisher.publish(selections, cap);
        log.info("[pipeline] publish requests completed - {} posts", count);
    }
}

