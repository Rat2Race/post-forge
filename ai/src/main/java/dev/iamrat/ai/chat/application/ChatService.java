package dev.iamrat.ai.chat.application;

import dev.iamrat.ai.search.application.SearchPort;
import dev.iamrat.ai.support.application.AiSafetyGuard;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.ai.support.prompt.PromptResourceLoader;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String UNAVAILABLE_RESPONSE = "요청을 처리할 수 없습니다.";
    private static final String SAFETY_POLICY_PROMPT_PATH = "prompts/safety-policy.md";
    private static final String CHAT_SYSTEM_PROMPT_PATH = "prompts/chat-system.md";
    private static final String NO_RELATED_CONTEXT_SUFFIX = """

        관련 참고 문서 없음:
        검색은 정상 처리되었지만 질문과 직접 관련된 저장 문서를 찾지 못했습니다. 저장 문서에 근거가 없는 구체적인 사실은 단정하지 말고, 일반 정보로 답변할 때는 근거 문서가 없음을 밝혀주세요.""";

    private final TextGenerationClient textGenerationClient;
    private final PromptResourceLoader promptResourceLoader;
    private final SearchPort searchPort;
    private final AiSafetyGuard aiSafetyGuard;

    public String chat(String message, Long accountId, String clientIp) {

        // 부적절한 요청 처리
        if (aiSafetyGuard.shouldRefuse(message)) {
            log.warn(
                "AI 채팅 요청 거절: accountId={}, clientIp={}, reason=safety_policy",
                accountId,
                clientIp
            );
            return aiSafetyGuard.refusalMessage();
        }

        // 유사 문서 검색
        List<String> relevantDocs = searchPort.searchSimilar(message, 5);

        // 답변 생성
        String response = textGenerationClient.generate(
            chatSystemPrompt(relevantDocs),
            message
        );

        // 부적절한 출력 처리
        response = aiSafetyGuard.sanitizeOutput(response);
        if (response == null || response.isBlank()) {
            response = UNAVAILABLE_RESPONSE;
        }

        log.info("AI 채팅 응답 완료 (참조 문서 {}건)", relevantDocs.size());

        return response;
    }

    private String chatSystemPrompt(List<String> context) {
        String contextSuffix = context.isEmpty()
            ? NO_RELATED_CONTEXT_SUFFIX
            : "\n\n참고할 컨텍스트:\n" + String.join(
                "\n\n",
                context
            );

        return String.join(
            "\n\n",
            promptResourceLoader.load(SAFETY_POLICY_PROMPT_PATH),
            promptResourceLoader.render(CHAT_SYSTEM_PROMPT_PATH, Map.of("contextSuffix", contextSuffix))
        ).trim();
    }
}
