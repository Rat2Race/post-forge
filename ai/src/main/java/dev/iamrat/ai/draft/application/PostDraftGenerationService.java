package dev.iamrat.ai.draft.application;

import dev.iamrat.ai.draft.presentation.dto.PostDraftGenerateRequest;
import dev.iamrat.ai.draft.presentation.dto.PostDraftResponse;
import dev.iamrat.ai.support.application.AiSafetyGuard;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.core.board.post.PostCategory;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostDraftGenerationService {

    private static final String DEFAULT_REFUSAL_TITLE = "AI draft unavailable";

    private final TextGenerationClient textGenerationClient;
    private final PostDraftPromptTemplate postDraftPromptTemplate;
    private final AiSafetyGuard aiSafetyGuard;

    public PostDraftResponse generate(PostDraftGenerateRequest request) {
        if (aiSafetyGuard.shouldRefuse(securityInputs(request))) {
            return refusalResponse(request);
        }

        String content = normalize(aiSafetyGuard.sanitizeOutput(textGenerationClient.generate(
            postDraftPromptTemplate.draftSystemPrompt(),
            userPrompt(request)
        )));
        if (content == null) {
            content = request.effectivePrompt();
        }

        String title = firstNonBlank(request.title(), deriveTitle(request, content));
        String summary = firstNonBlank(request.summary(), deriveSummary(content));
        List<String> tags = normalizeTags(request.tags());
        PostCategory category = request.category() == null ? PostCategory.GENERAL : request.category();

        return new PostDraftResponse(title, content, summary, tags, category);
    }

    private PostDraftResponse refusalResponse(PostDraftGenerateRequest request) {
        String refusal = aiSafetyGuard.refusalMessage();
        String requestedTitle = normalize(request.title());
        String title = requestedTitle == null || aiSafetyGuard.shouldRefuse(requestedTitle)
            ? DEFAULT_REFUSAL_TITLE
            : requestedTitle;
        PostCategory category = request.category() == null ? PostCategory.GENERAL : request.category();
        return new PostDraftResponse(title, refusal, refusal, List.of(), category);
    }

    private String userPrompt(PostDraftGenerateRequest request) {
        return """
            주제 또는 요청:
            %s

            희망 제목: %s
            요약 힌트: %s
            태그 힌트: %s
            게시판 카테고리: %s
            """.formatted(
            request.effectivePrompt(),
            valueOrDash(request.title()),
            valueOrDash(request.summary()),
            request.tags() == null || request.tags().isEmpty() ? "-" : String.join(", ", request.tags()),
            request.category() == null ? PostCategory.GENERAL : request.category()
        );
    }

    private String[] securityInputs(PostDraftGenerateRequest request) {
        return new String[] {
            request.prompt(),
            request.topic(),
            request.title(),
            request.summary(),
            request.tags() == null ? null : String.join(" ", request.tags())
        };
    }

    private String deriveTitle(PostDraftGenerateRequest request, String content) {
        String source = firstNonBlank(request.topic(), request.prompt(), firstLine(content));
        if (source == null) {
            return "AI generated draft";
        }
        return abbreviate(cleanTitle(source), 100);
    }

    private String deriveSummary(String content) {
        if (content == null) {
            return null;
        }
        return abbreviate(content.replaceAll("\\s+", " ").trim(), 500);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = normalize(tag);
            if (value != null) {
                normalized.add(value);
            }
            if (normalized.size() >= 20) {
                break;
            }
        }
        return List.copyOf(normalized);
    }

    private String firstLine(String content) {
        if (content == null) {
            return null;
        }
        int lineBreak = content.indexOf('\n');
        return lineBreak < 0 ? content : content.substring(0, lineBreak);
    }

    private String cleanTitle(String value) {
        return value.replaceFirst("^#+\\s*", "").replaceAll("\\s+", " ").trim();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalize(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return normalize(second);
    }

    private String firstNonBlank(String first, String second, String third) {
        return firstNonBlank(firstNonBlank(first, second), third);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String valueOrDash(String value) {
        String normalized = normalize(value);
        return normalized == null ? "-" : normalized;
    }
}
