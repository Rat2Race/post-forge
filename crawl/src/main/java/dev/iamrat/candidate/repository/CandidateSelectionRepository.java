package dev.iamrat.candidate.repository;

import dev.iamrat.candidate.entity.CandidateSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface CandidateSelectionRepository extends JpaRepository<CandidateSelection, Long> {
    void deleteByRunDate(LocalDate runDate);
    List<CandidateSelection> findByRunDateOrderByCreatedAtAsc(LocalDate runDate);
}

