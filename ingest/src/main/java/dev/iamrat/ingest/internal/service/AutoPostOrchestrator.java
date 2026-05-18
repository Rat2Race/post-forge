package dev.iamrat.ingest.internal.service;

import dev.iamrat.core.ai.post.NewsAnalysisPostPublisher;
import dev.iamrat.core.ai.post.NewsAnalysisPostRequest;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.ingest.document.dto.DocumentRequest;
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

    /**
     * 문서 적재 직후 실행되는 뉴스 기반 자동 포스팅 오케스트레이션.
     * 저장은 기준 상태로 유지하고, 자동 생성은 best-effort 후행 작업으로 분리한다.
     */
    public int publishEligible(List<DocumentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        Set<String> publishedLinks = new LinkedHashSet<>();

        for (DocumentRequest request : requests) {
            NewsDocumentMetadata metadata = NewsDocumentMetadata.from(request.source(), request.metadata())
                .filter(NewsDocumentMetadata::canPublishAutomatically)
                .orElse(null);
            if (metadata == null) {
                continue;
            }

            String originalLink = metadata.originalLink();
            if (originalLink == null || !publishedLinks.add(originalLink)) {
                continue;
            }

            try {
                newsAnalysisPostPublisher.publishNewsAnalysis(new NewsAnalysisPostRequest(
                    metadata.keyword(),
                    metadata.newsTitle(),
                    request.content(),
                    originalLink
                ));
                publishedCount++;
            } catch (Exception e) {
                log.error("문서 적재 후 뉴스 분석 게시글 생성 실패 - originalLink={}", originalLink, e);
            }
        }

        return publishedCount;
    }

    boolean isEligible(DocumentRequest request) {
        return NewsDocumentMetadata.from(request.source(), request.metadata())
            .map(NewsDocumentMetadata::canPublishAutomatically)
            .orElse(false);
    }
}
