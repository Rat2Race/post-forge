package dev.iamrat.ingest.pipeline.application;

import dev.iamrat.core.ai.post.NewsAnalysisPostPublisher;
import dev.iamrat.core.ai.post.NewsAnalysisPostRequest;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.ingest.pipeline.domain.IngestPolicy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPostOrchestrator {

    private final NewsAnalysisPostPublisher newsAnalysisPostPublisher;
    private final IngestPolicy ingestPolicy = new IngestPolicy();

    /**
     * 문서 적재 직후 실행되는 뉴스 기반 자동 포스팅 오케스트레이션.
     * 저장은 기준 상태로 유지하고, 자동 생성은 best-effort 후행 작업으로 분리한다.
     */
    public int publishEligible(List<DocumentIngestCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        Set<String> publishedLinks = new LinkedHashSet<>();

        for (DocumentIngestCommand command : commands) {
            NewsDocumentMetadata metadata = ingestPolicy.autoPostMetadata(command.source(), command.metadata())
                .orElse(null);
            if (metadata == null) {
                continue;
            }

            if (!ingestPolicy.reserveAutoPostOriginalLink(metadata, publishedLinks)) {
                continue;
            }

            try {
                newsAnalysisPostPublisher.publishNewsAnalysis(new NewsAnalysisPostRequest(
                    metadata.keyword(),
                    metadata.newsTitle(),
                    command.content(),
                    metadata.originalLink()
                ));
                publishedCount++;
            } catch (Exception e) {
                log.error("문서 적재 후 뉴스 분석 게시글 생성 실패 - originalLink={}", metadata.originalLink(), e);
            }
        }

        return publishedCount;
    }
}
