package dev.iamrat.ai.support.infrastructure.openai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpenAiTextGenerationAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatModel chatModel;

    @Test
    @DisplayName("system/user 프롬프트를 Spring AI Prompt로 변환해 ChatModel을 호출한다")
    void generate_buildsSpringAiPrompt() {
        OpenAiTextGenerationAdapter adapter = new OpenAiTextGenerationAdapter(chatModel);
        given(chatModel.call(any(Prompt.class)).getResult().getOutput().getText()).willReturn("응답");
        clearInvocations(chatModel);

        String response = adapter.generate("system", "user");

        assertThat(response).isEqualTo("응답");
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();
        assertThat(prompt.getInstructions()).hasSize(2);
        assertThat(prompt.getInstructions().getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(((SystemMessage) prompt.getInstructions().getFirst()).getText()).isEqualTo("system");
        assertThat(prompt.getInstructions().get(1)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) prompt.getInstructions().get(1)).getText()).isEqualTo("user");
    }
}
