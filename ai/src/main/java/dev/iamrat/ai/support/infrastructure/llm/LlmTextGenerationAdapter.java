package dev.iamrat.ai.support.infrastructure.llm;

import dev.iamrat.ai.support.application.TextGenerationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Spring AI 자동 구성(spring.ai.openai.*)이 만든 ChatModel로 부른다.
 * 호출 시간·토큰 수는 Spring AI 관측 지표(gen_ai.client.*)가 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmTextGenerationAdapter implements TextGenerationClient {

    private final ChatModel chatModel;

    // 실패하면 null을 돌려준다. 대체 처리는 호출자가 정한다.
    @Override
    public String generate(String systemPrompt, String userPrompt) {
        try {
            return chatModel.call(new SystemMessage(systemPrompt), new UserMessage(userPrompt));
        } catch (RuntimeException exception) {
            log.warn("text generation unavailable; using fallback content. reason={}", exception.getMessage());
            return null;
        }
    }
}
