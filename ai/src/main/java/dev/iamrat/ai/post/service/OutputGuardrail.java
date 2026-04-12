package dev.iamrat.ai.post.service;

import dev.iamrat.ai.post.dto.GeneratedPost;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OutputGuardrail {

    private static final List<String> BANNED_PHRASES = List.of(
        "지금 사야 한다",
        "급등 확실",
        "무조건 오른다",
        "매수 추천",
        "수익 보장"
    );

    private static final String DISCLAIMER =
        "\n\n---\nAI가 자동 생성한 분석입니다. 투자 판단의 근거로 사용하지 마세요. 모든 투자의 책임은 본인에게 있습니다.";

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
        if (content.contains("투자의 책임은 본인에게 있습니다")) {
            return content;
        }
        return content + DISCLAIMER;
    }
}
