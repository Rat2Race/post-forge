package dev.iamrat.ai.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import dev.iamrat.ai.support.application.AiSafetyGuard;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.LaunchNewsPostDraftCommand;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaunchNewsPostDraftGenerationServiceTest {

    @Mock
    private TextGenerationClient textGenerationClient;

    @Mock
    private AiSafetyGuard aiSafetyGuard;

    private LaunchNewsPostDraftGenerationService service;

    @BeforeEach
    void setUp() {
        lenient().when(aiSafetyGuard.sanitizeOutput(nullable(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service = new LaunchNewsPostDraftGenerationService(
            textGenerationClient,
            () -> "launch-news-system",
            aiSafetyGuard
        );
    }

    @Test
    @DisplayName("출시 뉴스 초안은 md 기반 시스템 프롬프트로 생성 클라이언트를 호출한다")
    void generate_returnsDraftFromAiContent() {
        given(textGenerationClient.generate(anyString(), anyString())).willReturn("갤럭시북 신제품 출시 소식입니다.");

        Optional<LaunchNewsPostDraft> draft = service.generate(command());

        assertThat(draft).isPresent();
        assertThat(draft.get().title()).isEqualTo("갤럭시북 신제품 출시");
        assertThat(draft.get().content()).isEqualTo("갤럭시북 신제품 출시 소식입니다.");
        assertThat(draft.get().summary()).isEqualTo("갤럭시북 신제품 출시 소식입니다.");
        assertThat(draft.get().tags()).contains("갤럭시북", "launch-news", "n.news.naver.com");
        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(textGenerationClient).generate(systemPromptCaptor.capture(), anyString());
        assertThat(systemPromptCaptor.getValue()).isEqualTo("launch-news-system");
    }

    @Test
    @DisplayName("생성 클라이언트가 실패하면 빈 결과를 반환한다")
    void generate_returnsEmptyWhenClientFails() {
        given(textGenerationClient.generate(anyString(), anyString())).willThrow(new IllegalStateException("boom"));

        assertThat(service.generate(command())).isEmpty();
    }

    private LaunchNewsPostDraftCommand command() {
        return new LaunchNewsPostDraftCommand(
            "갤럭시북",
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "https://n.news.naver.com/article/001/1",
            "n.news.naver.com",
            "Sun, 21 Jun 2026 10:00:00 +0900"
        );
    }
}
