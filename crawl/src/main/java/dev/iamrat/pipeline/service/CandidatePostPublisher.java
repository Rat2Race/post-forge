package dev.iamrat.pipeline.service;

import dev.iamrat.candidate.entity.CandidateSelection;
import dev.iamrat.common.InternalCrawlClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatePostPublisher {

    private final InternalCrawlClient internalCrawlClient;

    public int publish(List<CandidateSelection> selections, int limit) {
        if (selections == null || selections.isEmpty() || limit <= 0) {
            return 0;
        }

        int successCount = 0;
        for (CandidateSelection selection : selections.stream().limit(limit).toList()) {
            if (internalCrawlClient.requestPostGeneration(selection.getTicker(), selection.getStockName())) {
                successCount++;
            }
        }
        return successCount;
    }
}

