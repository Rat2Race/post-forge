package dev.iamrat.ai.support.infrastructure.openai;

import dev.iamrat.ai.support.application.TextGenerationClient;
import java.util.List;

import dev.iamrat.ai.support.infrastructure.openai.OpenAiTextGenerationMetrics.Observation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiTextGenerationAdapter implements TextGenerationClient {

    private final ChatModel chatModel;
    private final OpenAiTextGenerationMetrics metrics;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        Prompt prompt = new Prompt(List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(userPrompt)
        ));

        Observation observation = metrics.start(systemPrompt, userPrompt);

        try {
            ChatResponse chatResponse = chatModel.call(prompt);
            String response = chatResponse
                .getResult()
                .getOutput()
                .getText();
            metrics.recordSuccess(chatResponse, response);
            return response;
        } catch (RuntimeException exception) {
            metrics.recordDegraded();

            log.warn("{} text generation unavailable; using fallback content. reason={}",
                metrics.provider(), exception.getMessage());

            return null;
        } finally {
            observation.stop();
        }
    }
}
