package dev.iamrat.ai.generation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.generation.domain.GeneratedPost;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeneratedPostParser {

    private final ObjectMapper objectMapper;

    public GeneratedPost parse(String responseText) {
        try {
            String json = extractJson(responseText);
            return objectMapper.readValue(json, GeneratedPost.class);
        } catch (Exception e) {
            log.warn("AI 응답 JSON 파싱 실패, 기본 형식으로 변환: {}", e.getMessage());
            return fallback(responseText);
        }
    }

    private GeneratedPost fallback(String responseText) {
        return new GeneratedPost(
            "트렌드 분석",
            responseText.length() > 100 ? responseText.substring(0, 100) + "..." : responseText,
            responseText,
            List.of()
        );
    }

    private String extractJson(String text) {
        text = text.trim();

        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd + 1);
        }

        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
