package dev.iamrat.core.board.post;

import java.util.Optional;

public interface DailyDigestDraftGenerator {
    Optional<DailyDigestDraft> generate(DailyDigestDraftCommand command);
}
