package dev.iamrat.ai.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("당신은 PostForge 커뮤니티의 AI 어시스턴트입니다. 사용자의 질문에 친절하고 정확하게 답변해주세요.")
                .build();
    }

    public String chat(String message) {
        log.info("AI 채팅 요청: {}", message);

        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();

        log.info("AI 채팅 응답 완료");
        return response;
    }
}