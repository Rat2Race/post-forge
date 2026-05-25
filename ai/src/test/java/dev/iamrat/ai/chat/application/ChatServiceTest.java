package dev.iamrat.ai.chat.application;

import dev.iamrat.ai.search.application.SemanticSearchService;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.application.TextGenerationClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private TextGenerationClient textGenerationClient;

    @Mock
    private SemanticSearchService semanticSearchService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
            textGenerationClient,
            new TestChatPromptTemplate(),
            semanticSearchService
        );
    }

    @Test
    @DisplayName("채팅 프롬프트는 검색 컨텍스트를 포함해 생성 client로 전달한다")
    void chat_passesPromptWithSearchContext() {
        SearchResult contextDoc = new SearchResult("컨텍스트 문서", Map.of());
        given(semanticSearchService.searchSimilar("질문", 5)).willReturn(List.of(contextDoc));
        given(textGenerationClient.generate(anyString(), anyString())).willReturn("응답");

        String response = chatService.chat("질문");

        assertThat(response).isEqualTo("응답");
        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(textGenerationClient).generate(systemPromptCaptor.capture(), userPromptCaptor.capture());

        assertThat(systemPromptCaptor.getValue())
            .contains("chat-system")
            .contains("참고할 컨텍스트:")
            .contains("컨텍스트 문서");
        assertThat(userPromptCaptor.getValue()).isEqualTo("질문");
    }

    @Test
    @DisplayName("관련 문서가 없으면 컨텍스트 섹션 없이 시스템 프롬프트를 전달한다")
    void chat_withoutContext_omitsContextSection() {
        given(semanticSearchService.searchSimilar("질문", 5)).willReturn(List.of());
        given(textGenerationClient.generate(anyString(), anyString())).willReturn("응답");

        chatService.chat("질문");

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(textGenerationClient).generate(systemPromptCaptor.capture(), anyString());

        assertThat(systemPromptCaptor.getValue())
            .contains("chat-system")
            .doesNotContain("참고할 컨텍스트:");
    }
}
