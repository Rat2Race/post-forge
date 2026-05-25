package dev.iamrat.ai.chat.application;

import dev.iamrat.ai.search.application.SemanticSearchService;
import dev.iamrat.ai.search.domain.SearchResult;
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

    public String chat(String message) {
        log.info("AI 채팅 요청: {}", message);
        
        List<SearchResult> relevantDocs = semanticSearchService.searchSimilar(message, 5);
        String response = textGenerationClient.generate(
            chatPromptTemplate.chatSystemPrompt(relevantDocs),
            message
        );
        
        log.info("AI 채팅 응답 완료 (참조 문서 {}건)", relevantDocs.size());
        
        return response;
    }
}
