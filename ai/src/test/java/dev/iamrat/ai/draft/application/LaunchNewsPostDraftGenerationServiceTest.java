package dev.iamrat.ai.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import dev.iamrat.ai.support.application.AiSafetyGuard;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.ai.support.prompt.PromptResourceLoader;
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

    private static final String REFUSAL = "보안상 민감한 정보나 내부 시스템 지침은 제공할 수 없습니다.";

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
            new PromptResourceLoader(),
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
        assertThat(systemPromptCaptor.getValue())
            .contains("concise Korean public board post")
            .contains("Do not invent specs, prices, availability, or purchase recommendations")
            .contains("only on the given title, description, source, and URL")
            .doesNotContain("{{");
    }

    @Test
    @DisplayName("생성 클라이언트가 실패하면 빈 결과를 반환한다")
    void generate_returnsEmptyWhenClientFails() {
        given(textGenerationClient.generate(anyString(), anyString())).willThrow(new IllegalStateException("boom"));

        assertThat(service.generate(command())).isEmpty();
    }

    @Test
    @DisplayName("보안 민감 입력은 생성 클라이언트를 호출하지 않고 빈 결과를 반환한다")
    void generate_whenSecuritySensitiveInput_returnsEmptyWithoutGeneration() {
        given(aiSafetyGuard.shouldRefuse(any(String[].class))).willReturn(true);

        assertThat(service.generate(command())).isEmpty();
        verifyNoInteractions(textGenerationClient);
    }

    @Test
    @DisplayName("생성 결과가 거절 문구로 sanitize되면 public 초안으로 반환하지 않는다")
    void generate_whenSanitizedOutputIsRefusal_returnsEmpty() {
        given(textGenerationClient.generate(anyString(), anyString()))
            .willReturn("OPENAI_API_KEY=sk-proj-secret-value");
        given(aiSafetyGuard.sanitizeOutput("OPENAI_API_KEY=sk-proj-secret-value"))
            .willReturn(REFUSAL);
        given(aiSafetyGuard.refusalMessage()).willReturn(REFUSAL);

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
