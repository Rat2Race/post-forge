package dev.iamrat.catalog.matching.application;

import dev.iamrat.catalog.matching.domain.ProductMatchCandidate;
import dev.iamrat.catalog.matching.domain.ProductMatchStatus;
import dev.iamrat.catalog.matching.infrastructure.persistence.ProductMatchCandidateRepository;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductMatchCandidateService {

    private final ProductMatchCandidateRepository candidateRepository;

    public Page<ProductMatchCandidate> find(ProductMatchStatus status, Pageable pageable) {
        if (status == null) {
            return candidateRepository.findAll(pageable);
        }
        return candidateRepository.findByStatus(status, pageable);
    }

    @Transactional
    public void approve(Long candidateId) {
        getById(candidateId).approve();
    }

    @Transactional
    public void reject(Long candidateId) {
        getById(candidateId).reject();
    }

    private ProductMatchCandidate getById(Long candidateId) {
        return candidateRepository.findById(candidateId)
            .orElseThrow(() -> new CustomException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }
}
