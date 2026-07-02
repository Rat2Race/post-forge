package dev.iamrat.ingest.product.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.ingest.product.domain.TrackedKeyword;
import dev.iamrat.ingest.product.infrastructure.persistence.TrackedKeywordRepository;
import dev.iamrat.source.product.domain.SourceType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackedKeywordService {

    private final TrackedKeywordRepository trackedKeywordRepository;

    @Transactional
    public TrackedKeyword register(SourceType source, String keyword, Integer intervalMinutes, Integer displayCount) {
        SourceType effectiveSource = source == null ? SourceType.MOCK : source;
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return trackedKeywordRepository.findBySourceAndKeyword(effectiveSource, normalizedKeyword)
            .orElseGet(() -> trackedKeywordRepository.save(
                TrackedKeyword.register(effectiveSource, normalizedKeyword, intervalMinutes, displayCount)
            ));
    }

    public List<TrackedKeyword> findAll() {
        return trackedKeywordRepository.findAll();
    }

    public List<TrackedKeyword> findActive() {
        return trackedKeywordRepository.findByEnabledTrue();
    }

    @Transactional
    public void disable(Long id) {
        TrackedKeyword keyword = trackedKeywordRepository.findById(id)
            .orElseThrow(() -> new CustomException(CommonErrorCode.RESOURCE_NOT_FOUND));
        keyword.disable();
    }
}
