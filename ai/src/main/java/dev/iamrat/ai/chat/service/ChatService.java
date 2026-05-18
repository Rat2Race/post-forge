package dev.iamrat.ai.chat.service;

import dev.iamrat.ai.prompt.PromptTemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String CHAT_SYSTEM_PROMPT_PATH = "prompts/chat-system.md";

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final PromptTemplateLoader promptTemplateLoader;

    public String chat(String message) {
        log.info("AI 채팅 요청: {}", message);
        
        List<Document> relevantDocs = getDocuments(message);

        String contextSuffix = relevantDocs.isEmpty()
            ? ""
            : "\n\n참고할 컨텍스트:\n" + relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = promptTemplateLoader.render(
            CHAT_SYSTEM_PROMPT_PATH,
            java.util.Map.of("contextSuffix", contextSuffix)
        );

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(message)
        ));
        
        String response = getResponse(prompt);
        
        log.info("AI 채팅 응답 완료 (참조 문서 {}건)", relevantDocs.size());
        
        return response;
    }
    
    private String getResponse(Prompt prompt) {
        return chatModel.call(prompt)
            .getResult()
            .getOutput()
            .getText();
    }
    
    private List<Document> getDocuments(String message) {
        return vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(message)
                .topK(5)
                .build()
        );
    }
}
