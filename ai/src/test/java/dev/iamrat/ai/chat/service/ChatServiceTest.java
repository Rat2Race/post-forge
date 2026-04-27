package dev.iamrat.ai.chat.service;

import dev.iamrat.ai.prompt.PromptTemplateLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatModel chatModel;

    @Mock
    private VectorStore vectorStore;

    @Spy
    private PromptTemplateLoader promptTemplateLoader = new PromptTemplateLoader();

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("채팅 프롬프트는 markdown 리소스를 읽고 컨텍스트를 덧붙인다")
    void chat_loadsMarkdownPromptAndAppendsContext() {
        Document contextDoc = new Document("컨텍스트 문서");
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of(contextDoc));
        given(chatModel.call(any(Prompt.class)).getResult().getOutput().getText()).willReturn("응답");

        String response = chatService.chat("질문");

        assertThat(response).isEqualTo("응답");
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, atLeastOnce()).call(captor.capture());

        Prompt prompt = captor.getAllValues().getLast();
        assertThat(prompt.getInstructions()).hasSize(2);
        assertThat(prompt.getInstructions().getFirst()).isInstanceOf(SystemMessage.class);
        SystemMessage systemMessage = (SystemMessage) prompt.getInstructions().getFirst();
        assertThat(systemMessage.getText()).isEqualTo("""
            당신은 PostForge 커뮤니티의 AI 어시스턴트입니다.
            사용자의 질문에 친절하고 정확하게 답변해주세요.
            제공된 컨텍스트가 있다면 이를 기반으로 답변하세요.

            참고할 컨텍스트:
            컨텍스트 문서""");
    }

    @Test
    @DisplayName("관련 문서가 없으면 기존 기본 시스템 프롬프트와 동일하게 동작한다")
    void chat_withoutContext_keepsBasePrompt() {
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(chatModel.call(any(Prompt.class)).getResult().getOutput().getText()).willReturn("응답");

        chatService.chat("질문");

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, atLeastOnce()).call(captor.capture());

        Prompt prompt = captor.getAllValues().getLast();
        SystemMessage systemMessage = (SystemMessage) prompt.getInstructions().getFirst();
        assertThat(systemMessage.getText()).isEqualTo("""
            당신은 PostForge 커뮤니티의 AI 어시스턴트입니다.
            사용자의 질문에 친절하고 정확하게 답변해주세요.
            제공된 컨텍스트가 있다면 이를 기반으로 답변하세요.""");
    }
}
