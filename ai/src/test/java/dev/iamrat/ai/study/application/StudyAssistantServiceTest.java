package dev.iamrat.ai.study.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.ai.support.prompt.PromptResourceLoader;
import dev.iamrat.core.study.QuestionDraft;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StudyAssistantServiceTest {

    private final FakeTextGenerationClient model = new FakeTextGenerationClient();
    private final StudyAssistantService assistant = new StudyAssistantService(model, new PromptResourceLoader());

    @Test
    @DisplayName("모델이 코드 블록과 군말을 덧붙여도 JSON 배열의 질문과 근거만 꺼낸다")
    void extractsDraftsFromNoisyOutput() {
        model.response = """
            네, 문제를 만들었습니다.
            ```json
            [{"question": "왜 두 번 읽은 결과가 다를 수 있나요?", "evidence": "문장마다 새 스냅샷을 쓴다", "level": 2}]
            ```
            """;

        assertThat(assistant.draftQuestions("자료", 3))
            .containsExactly(new QuestionDraft("왜 두 번 읽은 결과가 다를 수 있나요?", "문장마다 새 스냅샷을 쓴다"));
    }

    @Test
    @DisplayName("모델 응답이 없거나 JSON이 아니면 빈 목록을 돌려준다")
    void returnsEmptyOnMissingOrBrokenOutput() {
        model.response = null;
        assertThat(assistant.draftQuestions("자료", 3)).isEmpty();

        model.response = "[{\"question\": \"끊긴 응답";
        assertThat(assistant.draftQuestions("자료", 3)).isEmpty();
        assertThat(assistant.askAsStudent("자료", "설명", 3)).isEmpty();
    }

    @Test
    @DisplayName("학생 질문은 문자열 배열로 꺼낸다")
    void extractsStudentQuestions() {
        model.response = "[\"스냅샷은 언제 찍나요?\", \"예를 들어 줄 수 있나요?\"]";

        assertThat(assistant.askAsStudent("자료", "설명", 3))
            .containsExactly("스냅샷은 언제 찍나요?", "예를 들어 줄 수 있나요?");
    }

    @Test
    @DisplayName("꼬리질문은 자료 일부·앞 문제·앞 근거를 보내고, 질문과 근거 배열을 꺼낸다")
    void draftsFollowUpsFromContextAndParentQuestion() {
        model.response = "[{\"question\": \"왜 문장마다 스냅샷을 새로 쓰나요?\", \"evidence\": \"문장마다 새 스냅샷을 쓴다\"}]";

        assertThat(assistant.draftFollowUps("자료 일부", "무엇을 읽나요?", "커밋된 데이터만 읽는다", 1))
            .containsExactly(new QuestionDraft("왜 문장마다 스냅샷을 새로 쓰나요?", "문장마다 새 스냅샷을 쓴다"));
        assertThat(model.lastUserPrompt).contains("자료 일부", "무엇을 읽나요?", "커밋된 데이터만 읽는다");
    }

    @Test
    @DisplayName("긴 자료는 앞부분만 모델에 보낸다")
    void clipsLongContentBeforeSending() {
        model.response = "[]";

        assistant.draftQuestions("가".repeat(20_000), 3);

        assertThat(model.lastUserPrompt).hasSizeLessThan(5_000);
    }

    private static class FakeTextGenerationClient implements TextGenerationClient {

        String response;
        String lastUserPrompt;

        @Override
        public String generate(String systemPrompt, String userPrompt) {
            lastUserPrompt = userPrompt;
            return response;
        }
    }
}
