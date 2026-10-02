package dev.iamrat.study.domain;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.study.support.error.StudyErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySourceRepository extends JpaRepository<StudySource, Long> {

    Optional<StudySource> findByIdAndOwnerAccountId(Long id, Long ownerAccountId);

    List<StudySource> findByOwnerAccountIdOrderByIdDesc(Long ownerAccountId);

    List<StudySource> findByOwnerAccountIdAndRecallDueAtLessThanOrderByRecallDueAtAscIdAsc(
        Long ownerAccountId, LocalDateTime before);

    /** 남의 자료는 403이 아니라 404로 숨긴다. 있는지조차 알리지 않는다. */
    default StudySource getOwned(Long id, Long ownerAccountId) {
        return findByIdAndOwnerAccountId(id, ownerAccountId)
            .orElseThrow(() -> new CustomException(StudyErrorCode.SOURCE_NOT_FOUND));
    }
}
