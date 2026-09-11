package dev.iamrat.ai.draft.application;

import dev.iamrat.ai.support.application.AiSafetyGuard;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.ai.support.prompt.PromptResourceLoader;
import dev.iamrat.core.board.post.DailyDigestDraft;
import dev.iamrat.core.board.post.DailyDigestDraftCommand;
import dev.iamrat.core.board.post.DailyDigestDraftGenerator;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyDigestDraftGenerationService implements DailyDigestDraftGenerator {

    private static final String DAILY_DIGEST_SYSTEM_PROMPT_PATH = "prompts/daily-digest-system.md";

    private final TextGenerationClient textGenerationClient;
    private final PromptResourceLoader promptResourceLoader;
    private final AiSafetyGuard aiSafetyGuard;

    @Override
    public Optional<DailyDigestDraft> generate(DailyDigestDraftCommand command) {
        if (aiSafetyGuard.shouldRefuse(securityInputs(command))) {
            return Optional.empty();
        }

        try {
            String content = normalize(aiSafetyGuard.sanitizeOutput(textGenerationClient.generate(
                promptResourceLoader.load(DAILY_DIGEST_SYSTEM_PROMPT_PATH),
                userPrompt(command)
            )));
            if (content == null || isRefusalMessage(content) || aiSafetyGuard.shouldRefuse(content)) {
                return Optional.empty();
            }
            return Optional.of(new DailyDigestDraft(
                DraftText.abbreviate(content, DraftText.MAX_CONTENT_LENGTH),
                tags(command)
            ));
        } catch (RuntimeException ex) {
            log.warn("Daily digest draft generation failed. category={}, newsDate={}",
                command.category(), command.newsDate(), ex);
            return Optional.empty();
        }
    }

    private boolean isRefusalMessage(String content) {
        String refusal = normalize(aiSafetyGuard.refusalMessage());
        return refusal != null && refusal.equals(content);
    }

    private String userPrompt(DailyDigestDraftCommand command) {
        StringBuilder builder = new StringBuilder()
            .append("Category: ").append(command.category()).append('\n')
            .append("News date: ").append(command.newsDate()).append('\n')
            .append("Articles:\n");
        List<DailyDigestSourceItem> items = command.items();
        for (int i = 0; i < items.size(); i++) {
            DailyDigestSourceItem item = items.get(i);
            builder.append(i + 1).append(". ").append(item.title())
                .append(" - ").append(item.summary()).append('\n');
        }
        return builder.toString();
    }

    private String[] securityInputs(DailyDigestDraftCommand command) {
        return command.items().stream()
            .flatMap(item -> Stream.of(item.title(), item.summary()))
            .toArray(String[]::new);
    }

    private List<String> tags(DailyDigestDraftCommand command) {
        return List.of(command.category().name().toLowerCase(Locale.ROOT), "daily-digest");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
