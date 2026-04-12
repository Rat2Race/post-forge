package dev.iamrat.crawl.candidate.repository;

import dev.iamrat.crawl.candidate.entity.CandidateSelection;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateSelectionRepository extends JpaRepository<CandidateSelection, Long> {
    void deleteByRunDate(LocalDate runDate);
    List<CandidateSelection> findByRunDateOrderByCreatedAtAsc(LocalDate runDate);
}
