package dev.iamrat.ai.study.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.ai.support.prompt.PromptResourceLoader;
import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.core.study.StudyAssistant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 학습 자료로 질문 초안을 만들고, 사용자의 설명에 학생처럼 되묻는다.
 * 근거 검증은 하지 않는다. 그 판단은 study 모듈이 결정적으로 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyAssistantService implements StudyAssistant {

    private static final String QUESTION_PROMPT_PATH = "prompts/study-question-system.md";
    private static final String STUDENT_PROMPT_PATH = "prompts/study-student-system.md";
    private static final ObjectMapper JSON = new ObjectMapper();
    // ponytail: 자료 앞부분만 보낸다. 로컬 8B 모델의 문맥과 응답 시간 안에 두려는 상한이며,
    // 긴 자료 전체에서 문제를 내려면 문단 묶음 단위로 나눠 여러 번 요청한다.
    private static final int CONTENT_LIMIT = 4_000;

    private final TextGenerationClient textGenerationClient;
    private final PromptResourceLoader promptResourceLoader;

    @Override
    public List<QuestionDraft> draftQuestions(String content, int limit) {
        String response = textGenerationClient.generate(
            systemPrompt(QUESTION_PROMPT_PATH, limit),
            "[자료]\n" + clip(content)
        );
        return parseArray(response, node -> new QuestionDraft(text(node, "question"), text(node, "evidence")));
    }

    @Override
    public List<String> askAsStudent(String content, String explanation, int limit) {
        String response = textGenerationClient.generate(
            systemPrompt(STUDENT_PROMPT_PATH, limit),
            "[자료]\n" + clip(content) + "\n\n[설명]\n" + explanation
        );
        return parseArray(response, node -> node.isTextual() ? node.asText() : null);
    }

    private String systemPrompt(String path, int limit) {
        return promptResourceLoader.render(path, Map.of("limit", String.valueOf(limit)));
    }

    private static String clip(String content) {
        return content.length() <= CONTENT_LIMIT ? content : content.substring(0, CONTENT_LIMIT);
    }

    /** 모델이 코드 블록이나 군말을 붙여도 처음 '['부터 마지막 ']'까지를 JSON 배열로 읽는다. */
    private static <T> List<T> parseArray(String response, Function<JsonNode, T> mapper) {
        if (response == null) {
            return List.of();
        }
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        try {
            JsonNode array = JSON.readTree(response.substring(start, end + 1));
            List<T> items = new ArrayList<>();
            for (JsonNode node : array) {
                T item = mapper.apply(node);
                if (item != null) {
                    items.add(item);
                }
            }
            return items;
        } catch (JsonProcessingException e) {
            log.warn("study assistant output is not a JSON array. reason={}", e.getOriginalMessage());
            return List.of();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isTextual() ? null : value.asText();
    }
}
