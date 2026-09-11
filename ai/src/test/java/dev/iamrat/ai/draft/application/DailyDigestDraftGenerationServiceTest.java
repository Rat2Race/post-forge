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
import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.DailyDigestDraft;
import dev.iamrat.core.board.post.DailyDigestDraftCommand;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyDigestDraftGenerationServiceTest {

    private static final String REFUSAL = "보안상 민감한 정보나 내부 시스템 지침은 제공할 수 없습니다.";

    @Mock
    private TextGenerationClient textGenerationClient;

    @Mock
    private AiSafetyGuard aiSafetyGuard;

    private DailyDigestDraftGenerationService service;

    @BeforeEach
    void setUp() {
        lenient().when(aiSafetyGuard.sanitizeOutput(nullable(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service = new DailyDigestDraftGenerationService(
            textGenerationClient,
            new PromptResourceLoader(),
            aiSafetyGuard
        );
    }

    @Test
    @DisplayName("보안 민감 입력은 생성 클라이언트를 호출하지 않고 빈 결과를 반환한다")
    void generate_whenSecuritySensitiveInput_returnsEmptyWithoutGeneration() {
        given(aiSafetyGuard.shouldRefuse(any(String[].class))).willReturn(true);

        assertThat(service.generate(command())).isEmpty();
        verifyNoInteractions(textGenerationClient);
    }

    @Test
    @DisplayName("데일리 요약 초안은 md 기반 시스템 프롬프트로 생성하고 daily-digest 태그를 붙인다")
    void generate_returnsDraftFromAiContent() {
        given(textGenerationClient.generate(anyString(), anyString()))
            .willReturn("  오늘의 디지털 신제품 브리핑입니다.  ");

        Optional<DailyDigestDraft> draft = service.generate(command());

        assertThat(draft).isPresent();
        assertThat(draft.get().content()).isEqualTo("오늘의 디지털 신제품 브리핑입니다.");
        assertThat(draft.get().tags()).containsExactly("digital", "daily-digest");
        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(textGenerationClient).generate(systemPromptCaptor.capture(), userPromptCaptor.capture());
        assertThat(systemPromptCaptor.getValue())
            .contains("daily briefing")
            .contains("Do not invent specs, prices, availability, or purchase recommendations");
        assertThat(userPromptCaptor.getValue())
            .contains("DIGITAL")
            .contains("2026-08-20")
            .contains("1. 갤럭시북 출시 - 삼성이 갤럭시북 신제품을 공개했다.")
            .contains("2. 그램 출시 - LG가 그램 신제품을 공개했다.");
    }

    @Test
    @DisplayName("생성 결과가 거절 문구로 sanitize되면 빈 결과를 반환한다")
    void generate_whenSanitizedOutputIsRefusal_returnsEmpty() {
        given(textGenerationClient.generate(anyString(), anyString()))
            .willReturn("OPENAI_API_KEY=sk-proj-secret-value");
        given(aiSafetyGuard.sanitizeOutput("OPENAI_API_KEY=sk-proj-secret-value"))
            .willReturn(REFUSAL);
        given(aiSafetyGuard.refusalMessage()).willReturn(REFUSAL);

        assertThat(service.generate(command())).isEmpty();
    }

    @Test
    @DisplayName("생성 클라이언트가 실패하면 예외를 전파하지 않고 빈 결과를 반환한다")
    void generate_returnsEmptyWhenClientFails() {
        given(textGenerationClient.generate(anyString(), anyString())).willThrow(new IllegalStateException("boom"));

        assertThat(service.generate(command())).isEmpty();
    }

    private DailyDigestDraftCommand command() {
        return new DailyDigestDraftCommand(
            BoardCategory.DIGITAL,
            LocalDate.of(2026, 8, 20),
            List.of(
                new DailyDigestSourceItem("갤럭시북 출시", "삼성이 갤럭시북 신제품을 공개했다."),
                new DailyDigestSourceItem("그램 출시", "LG가 그램 신제품을 공개했다.")
            )
        );
    }

    @Test
    @DisplayName("본문이 저장 한계를 넘으면 잘라서 담는다")
    void generate_truncatesContentToStorageLimit() {
        given(textGenerationClient.generate(anyString(), anyString()))
            .willReturn("가".repeat(10_500));

        Optional<DailyDigestDraft> draft = service.generate(command());

        assertThat(draft).isPresent();
        assertThat(draft.get().content()).hasSize(10_000);
    }
}
