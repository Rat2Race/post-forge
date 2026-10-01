package dev.iamrat.study.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyQuestionRepository extends JpaRepository<StudyQuestion, Long> {

    Optional<StudyQuestion> findByIdAndOwnerAccountId(Long id, Long ownerAccountId);

    List<StudyQuestion> findBySourceIdOrderById(Long sourceId);

    List<StudyQuestion> findTop20ByOwnerAccountIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
        Long ownerAccountId, LocalDateTime now);
}
