package dev.iamrat.ai.chat.service;

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

    private static final String SYSTEM_PROMPT = """
            당신은 PostForge 커뮤니티의 AI 어시스턴트입니다.
            사용자의 질문에 친절하고 정확하게 답변해주세요.
            제공된 컨텍스트가 있다면 이를 기반으로 답변하세요.""";

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    public String chat(String message) {
        log.info("AI 채팅 요청: {}", message);
        
        List<Document> relevantDocs = getDocuments(message);
        
        String systemPrompt = SYSTEM_PROMPT;
        
        if (!relevantDocs.isEmpty()) {
            String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
            
            systemPrompt += "\n\n참고할 컨텍스트:\n" + context;
        }

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(message)
        ));
        
        String response = getResponse(prompt);
        
        log.info("AI 채팅 응답 완료 (참조 문서 {}건)", relevantDocs.size());
        
        return response;
    }
    
    private String getResponse(Prompt prompt) {
        String response = chatModel.call(prompt)
            .getResult()
            .getOutput()
            .getText();
        return response;
    }
    
    private List<Document> getDocuments(String message) {
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(message)
                .topK(5)
                .build()
        );
        return relevantDocs;
    }
    
    
}