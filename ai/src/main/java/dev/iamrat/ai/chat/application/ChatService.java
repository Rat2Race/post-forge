package dev.iamrat.ai.chat.application;

import dev.iamrat.ai.search.application.SemanticSearchService;
import dev.iamrat.ai.search.application.SearchOutcome;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.application.AiSafetyGuard;
import dev.iamrat.ai.support.application.TextGenerationClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String UNAVAILABLE_RESPONSE = "요청을 처리할 수 없습니다.";

    private final TextGenerationClient textGenerationClient;
    private final ChatPromptTemplate chatPromptTemplate;
    private final SemanticSearchService semanticSearchService;
    private final AiSafetyGuard aiSafetyGuard;

    public String chat(String message) {

        // 부적절한 요청 처리
        if (aiSafetyGuard.shouldRefuse(message)) {
            log.warn("AI 채팅 보안 민감 요청을 고정 거절 응답으로 처리했습니다.");
            return aiSafetyGuard.refusalMessage();
        }

        log.info("AI 채팅 요청: {}", message);

        // 유사 문서 검색
        SearchOutcome searchOutcome = semanticSearchService.searchSimilar(message, 5);
        if (searchOutcome.unavailable()) {
            log.warn("AI 채팅 검색 실패로 응답 생성을 중단합니다. reason={}", searchOutcome.failureReason());
            return UNAVAILABLE_RESPONSE;
        }

        // 답변 생성
        List<SearchResult> relevantDocs = searchOutcome.results();
        String response = textGenerationClient.generate(
            chatPromptTemplate.chatSystemPrompt(relevantDocs),
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
}
