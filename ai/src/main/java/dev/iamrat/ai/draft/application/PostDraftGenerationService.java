package dev.iamrat.ai.draft.application;

import dev.iamrat.ai.draft.presentation.dto.PostDraftGenerateRequest;
import dev.iamrat.ai.draft.presentation.dto.PostDraftResponse;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.core.board.post.PostCategory;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostDraftGenerationService {

    private static final String SYSTEM_PROMPT = """
        당신은 PostForge 게시판 초안 작성 도우미입니다.
        사용자가 제공한 주제와 힌트를 바탕으로 바로 게시글 본문에 붙여 넣을 수 있는 한국어 초안을 작성하세요.
        제목, 태그, JSON, 설명 문구를 따로 출력하지 말고 본문만 출력하세요.
        """;

    private final TextGenerationClient textGenerationClient;

    public PostDraftResponse generate(PostDraftGenerateRequest request) {
        String content = normalize(textGenerationClient.generate(SYSTEM_PROMPT, userPrompt(request)));
        if (content == null) {
            content = request.effectivePrompt();
        }

        String title = firstNonBlank(request.title(), deriveTitle(request, content));
        String summary = firstNonBlank(request.summary(), deriveSummary(content));
        List<String> tags = normalizeTags(request.tags());
        PostCategory category = request.category() == null ? PostCategory.GENERAL : request.category();

        return new PostDraftResponse(title, content, summary, tags, category);
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
