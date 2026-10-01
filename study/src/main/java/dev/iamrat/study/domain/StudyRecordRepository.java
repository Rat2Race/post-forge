package dev.iamrat.study.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    List<StudyRecord> findTop50ByOwnerAccountIdOrderByIdDesc(Long ownerAccountId);
}
