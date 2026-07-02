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

    private final TextGenerationClient textGenerationClient;
    private final ChatPromptTemplate chatPromptTemplate;
    private final SemanticSearchService semanticSearchService;
    private final AiSafetyGuard aiSafetyGuard;

    public String chat(String message) {
        if (aiSafetyGuard.shouldRefuse(message)) {
            log.warn("AI 채팅 보안 민감 요청을 고정 거절 응답으로 처리했습니다.");
            return aiSafetyGuard.refusalMessage();
        }

        log.info("AI 채팅 요청: {}", message);
        
        SearchOutcome searchOutcome = semanticSearchService.searchSimilar(message, 5);
        if (searchOutcome.unavailable()) {
            log.warn("AI 채팅 검색 실패로 응답 생성을 중단합니다. reason={}", searchOutcome.failureReason());
            return searchUnavailableResponse();
        }

        List<SearchResult> relevantDocs = searchOutcome.results();
        String response = textGenerationClient.generate(
            chatPromptTemplate.chatSystemPrompt(relevantDocs),
            message
        );
        response = aiSafetyGuard.sanitizeOutput(response);
        if (response == null || response.isBlank()) {
            response = fallbackResponse(relevantDocs);
        }
        
        log.info("AI 채팅 응답 완료 (참조 문서 {}건)", relevantDocs.size());
        
        return response;
    }

    private String searchUnavailableResponse() {
        return "AI 답변에 필요한 참고 문서 검색이 일시적으로 불가해 응답 생성을 중단했습니다. 잠시 후 다시 시도해주세요.";
    }

    private String fallbackResponse(List<SearchResult> relevantDocs) {
        if (relevantDocs.isEmpty()) {
            return "AI 응답 생성이 일시적으로 실패했습니다. 요청은 정상 처리되었고, 잠시 후 다시 시도해주세요.";
        }
        return "AI 응답 생성이 일시적으로 실패했습니다. 저장된 참고 문서는 확인되었지만 답변 생성은 잠시 후 다시 시도해주세요.";
    }
}
