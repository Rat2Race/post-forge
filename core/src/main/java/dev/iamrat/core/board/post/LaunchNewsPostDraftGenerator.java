package dev.iamrat.core.board.post;

import java.util.Optional;

public interface LaunchNewsPostDraftGenerator {
    Optional<LaunchNewsPostDraft> generate(LaunchNewsPostDraftCommand command);
}
