package dev.iamrat.board.autopost.infrastructure.persistence;

import dev.iamrat.board.autopost.domain.AutoPostDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoPostDraftRepository extends JpaRepository<AutoPostDraft, Long> {
    boolean existsByEventId(String eventId);
}
