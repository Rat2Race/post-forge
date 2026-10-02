package dev.iamrat.study.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyQuestionRepository extends JpaRepository<StudyQuestion, Long> {

    Optional<StudyQuestion> findByIdAndOwnerAccountId(Long id, Long ownerAccountId);

    List<StudyQuestion> findBySourceIdOrderById(Long sourceId);

    // ponytail: 하루치 후보를 200개까지 읽어 메모리에서 섞는다. 밀린 문제가 수백 개면 날짜 범위를 나눠 읽는다.
    List<StudyQuestion> findTop200ByOwnerAccountIdAndDueAtLessThanOrderByDueAtAscIdAsc(
        Long ownerAccountId, LocalDateTime before);

    /** 행: [상자(Integer), 문제 수(Long)] */
    @Query("select q.box, count(q) from StudyQuestion q where q.ownerAccountId = :owner group by q.box")
    List<Object[]> countByBox(@Param("owner") Long ownerAccountId);
}
