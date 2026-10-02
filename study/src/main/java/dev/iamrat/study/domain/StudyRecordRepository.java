package dev.iamrat.study.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    List<StudyRecord> findTop50ByOwnerAccountIdOrderByIdDesc(Long ownerAccountId);

    @Query("select r.createdAt from StudyRecord r "
        + "where r.ownerAccountId = :owner and r.createdAt >= :from and r.kind in :kinds")
    List<LocalDateTime> findActivityTimes(@Param("owner") Long ownerAccountId, @Param("from") LocalDateTime from,
                                          @Param("kinds") Collection<StudyRecord.Kind> kinds);

    /** 행: [복습 직전 상자(Integer, 이전 기록은 null), 자가 평가(String), 수(Long)] */
    @Query("select r.reviewBox, r.result, count(r) from StudyRecord r "
        + "where r.ownerAccountId = :owner and r.kind = :kind group by r.reviewBox, r.result")
    List<Object[]> countAnswersByBoxAndResult(@Param("owner") Long ownerAccountId, @Param("kind") StudyRecord.Kind kind);
}
