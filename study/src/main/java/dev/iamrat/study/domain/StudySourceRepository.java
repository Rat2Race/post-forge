package dev.iamrat.study.domain;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.study.support.error.StudyErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudySourceRepository extends JpaRepository<StudySource, Long> {

    Optional<StudySource> findByIdAndOwnerAccountId(Long id, Long ownerAccountId);

    List<StudySource> findByOwnerAccountIdOrderByIdDesc(Long ownerAccountId);

    List<StudySource> findByOwnerAccountIdAndRecallDueAtLessThanOrderByRecallDueAtAscIdAsc(
        Long ownerAccountId, LocalDateTime before);

    /**
     * 행 하나: [LLM 문제 초안 수 합(Number), 근거 실패로 버린 수 합(Number)].
     * 초안 수를 남기기 전에 만든 자료는 버린 수만 있어 통과 수가 음수가 되므로 빼고 센다.
     */
    @Query("select coalesce(sum(s.draftedQuestionCount), 0), coalesce(sum(s.discardedQuestionCount), 0) "
        + "from StudySource s where s.ownerAccountId = :owner and s.draftedQuestionCount > 0")
    List<Object[]> sumDraftedAndDiscarded(@Param("owner") Long ownerAccountId);

    /** 남의 자료는 403이 아니라 404로 숨긴다. 있는지조차 알리지 않는다. */
    default StudySource getOwned(Long id, Long ownerAccountId) {
        return findByIdAndOwnerAccountId(id, ownerAccountId)
            .orElseThrow(() -> new CustomException(StudyErrorCode.SOURCE_NOT_FOUND));
    }
}
