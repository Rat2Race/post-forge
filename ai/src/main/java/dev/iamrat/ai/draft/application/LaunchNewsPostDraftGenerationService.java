package dev.iamrat.ai.draft.application;

import dev.iamrat.ai.search.application.SearchPort;
import dev.iamrat.ai.support.application.AiSafetyGuard;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.ai.support.prompt.PromptResourceLoader;
import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.LaunchNewsPostDraftCommand;
import dev.iamrat.core.board.post.LaunchNewsPostDraftGenerator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaunchNewsPostDraftGenerationService implements LaunchNewsPostDraftGenerator {

    private static final String LAUNCH_NEWS_DRAFT_SYSTEM_PROMPT_PATH = "prompts/launch-news-draft-system.md";
    private static final int RELATED_CONTEXT_LIMIT = 5;

    private final TextGenerationClient textGenerationClient;
    private final PromptResourceLoader promptResourceLoader;
    private final SearchPort searchPort;
    private final AiSafetyGuard aiSafetyGuard;

    @Override
    public Optional<LaunchNewsPostDraft> generate(LaunchNewsPostDraftCommand command) {
        if (aiSafetyGuard.shouldRefuse(securityInputs(command))) {
            return Optional.empty();
        }

        try {
            List<String> relevantDocs = searchPort.searchSimilar(searchQuery(command), RELATED_CONTEXT_LIMIT);
            if (aiSafetyGuard.shouldRefuse(relevantDocs.toArray(String[]::new))) {
                return Optional.empty();
            }
            String content = normalize(aiSafetyGuard.sanitizeOutput(textGenerationClient.generate(
                promptResourceLoader.load(LAUNCH_NEWS_DRAFT_SYSTEM_PROMPT_PATH),
                userPrompt(command, relevantDocs)
            )));
            if (content == null || isRefusalMessage(content) || aiSafetyGuard.shouldRefuse(content)) {
                return Optional.empty();
            }
            return Optional.of(new LaunchNewsPostDraft(
                abbreviate(cleanTitle(command.sourceTitle()), 100),
                abbreviate(content, DraftText.MAX_CONTENT_LENGTH),
                abbreviate(content.replaceAll("\\s+", " "), 500),
                tags(command)
            ));
        } catch (RuntimeException ex) {
            log.warn("Launch news draft generation failed. canonicalUrl={}", command.canonicalUrl(), ex);
            return Optional.empty();
        }
    }

    private boolean isRefusalMessage(String content) {
        String refusal = normalize(aiSafetyGuard.refusalMessage());
        return refusal != null && refusal.equals(content);
    }

    private String userPrompt(LaunchNewsPostDraftCommand command, List<String> relevantDocs) {
        return """
            Primary current news article:
            Keyword: %s
            Source title: %s
            Source description: %s
            Source name: %s
            Published at: %s
            Canonical URL: %s

            Historical supporting context:
            %s
            """.formatted(
            valueOrDash(command.keyword()),
            valueOrDash(command.sourceTitle()),
            valueOrDash(command.sourceDescription()),
            valueOrDash(command.sourceName()),
            valueOrDash(command.publishedAt()),
            valueOrDash(command.canonicalUrl()),
            relevantDocs.isEmpty()
                ? "No related stored context was found. Use only the primary current news article."
                : String.join("\n\n---\n\n", relevantDocs)
        );
    }

    private String searchQuery(LaunchNewsPostDraftCommand command) {
        return String.join(" ", Stream.of(command.keyword(), command.sourceTitle())
            .map(this::normalize)
            .filter(Objects::nonNull)
            .toList());
    }

    private String[] securityInputs(LaunchNewsPostDraftCommand command) {
        return new String[] {
            command.keyword(),
            command.sourceTitle(),
            command.sourceDescription(),
            command.canonicalUrl(),
            command.sourceName()
        };
    }

    private List<String> tags(LaunchNewsPostDraftCommand command) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        add(tags, command.keyword());
        tags.add("launch-news");
        add(tags, command.sourceName());
        return List.copyOf(tags);
    }

    private void add(LinkedHashSet<String> tags, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            tags.add(abbreviate(normalized, 50));
        }
    }

    private String cleanTitle(String value) {
        String normalized = normalize(value);
        return normalized == null ? "Product launch news" : normalized.replaceFirst("^#+\\s*", "");
    }

    private String abbreviate(String value, int maxLength) {
        return DraftText.abbreviate(value, maxLength);
    }

    private String valueOrDash(String value) {
        String normalized = normalize(value);
        return normalized == null ? "-" : normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
