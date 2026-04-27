package dev.iamrat.internal.service;

import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.ai.post.service.PostGenerationService;
import dev.iamrat.document.dto.DocumentRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPostOrchestrator {

    private static final String SOURCE_NAVER_NEWS = "naver-news";
    private static final String METADATA_KEYWORD = "keyword";
    private static final String METADATA_NEWS_TITLE = "newsTitle";
    private static final String METADATA_ORIGINAL_LINK = "originalLink";
    private static final String METADATA_AUTO_POST_ELIGIBLE = "autoPostEligible";

    private final PostGenerationService postGenerationService;

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
            if (!isEligible(request)) {
                continue;
            }

            String originalLink = metadataValue(request, METADATA_ORIGINAL_LINK);
            if (originalLink == null || !publishedLinks.add(originalLink)) {
                continue;
            }

            try {
                GeneratedPost post = postGenerationService.generateNewsAnalysis(
                    metadataValue(request, METADATA_KEYWORD),
                    metadataValue(request, METADATA_NEWS_TITLE),
                    request.content(),
                    originalLink
                );
                postGenerationService.publish(post);
                publishedCount++;
            } catch (Exception e) {
                log.error("문서 적재 후 뉴스 분석 게시글 생성 실패 - originalLink={}", originalLink, e);
            }
        }

        return publishedCount;
    }

    /**
     * 현재 트렌드 플랫폼 자동 포스팅 정책:
     * - 네이버 뉴스 소스만 허용
     * - 명시적 opt-in(autoPostEligible=true)인 신규 기사만 허용
     * - keyword/originalLink/newsTitle가 있어야 게시글 생성에 필요한 식별 정보가 충분하다고 본다
     */
    boolean isEligible(DocumentRequest request) {
        if (!SOURCE_NAVER_NEWS.equals(request.source())) {
            return false;
        }
        if (!Boolean.parseBoolean(metadataValue(request, METADATA_AUTO_POST_ELIGIBLE))) {
            return false;
        }
        return hasText(metadataValue(request, METADATA_KEYWORD))
            && hasText(metadataValue(request, METADATA_NEWS_TITLE))
            && hasText(metadataValue(request, METADATA_ORIGINAL_LINK));
    }

    private String metadataValue(DocumentRequest request, String key) {
        Map<String, String> metadata = request.metadata();
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
