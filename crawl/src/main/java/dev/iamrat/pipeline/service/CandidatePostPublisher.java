package dev.iamrat.crawl.pipeline.service;

import dev.iamrat.crawl.candidate.entity.CandidateSelection;
import dev.iamrat.crawl.common.AiDocumentSender;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CandidatePostPublisher {

    private final AiDocumentSender aiDocumentSender;

    public int publish(List<CandidateSelection> selections, int limit) {
        if (selections == null || selections.isEmpty() || limit <= 0) {
            return 0;
        }

        int successCount = 0;
        for (CandidateSelection selection : selections.stream().limit(limit).toList()) {
            if (aiDocumentSender.triggerPostGeneration(selection.getTicker(), selection.getStockName())) {
                successCount++;
            }
        }
        return successCount;
    }
}
