package dev.iamrat.ai.post.service;

import dev.iamrat.ai.post.dto.GeneratedPost;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OutputGuardrail {

    private static final List<String> BANNED_PHRASES = List.of(
        "지금 바로 타야 한다",
        "확정적 상승",
        "확실한 호재",
        "무조건 뜬다",
        "수익 보장"
    );

    private static final String DISCLAIMER =
        "\n\n---\nAI가 자동 생성한 트렌드 분석입니다. 사실 관계와 맥락을 다시 확인하세요.";

    public GeneratedPost sanitize(GeneratedPost post) {
        return new GeneratedPost(
            sanitizeText(post.title()),
            sanitizeText(post.summary()),
            appendDisclaimer(sanitizeText(post.content())),
            post.tags()
        );
    }

    private String sanitizeText(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String sanitized = input;
        for (String bannedPhrase : BANNED_PHRASES) {
            sanitized = sanitized.replace(bannedPhrase, "가능성 및 리스크를 함께 확인해야 합니다");
        }
        return sanitized;
    }

    private String appendDisclaimer(String content) {
        if (content == null || content.isBlank()) {
            return DISCLAIMER.trim();
        }
        if (content.contains("사실 관계와 맥락을 다시 확인하세요")) {
            return content;
        }
        return content + DISCLAIMER;
    }
}
