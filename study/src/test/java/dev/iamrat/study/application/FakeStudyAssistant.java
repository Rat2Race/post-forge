package dev.iamrat.study.application;

import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.core.study.StudyAssistant;
import java.util.List;

/** LLM 대신 테스트가 정한 답을 돌려준다. 빈 목록은 LLM 장애와 같다. */
class FakeStudyAssistant implements StudyAssistant {

    List<QuestionDraft> drafts = List.of();
    List<String> studentQuestions = List.of();

    @Override
    public List<QuestionDraft> draftQuestions(String content, int limit) {
        return drafts;
    }

    @Override
    public List<String> askAsStudent(String content, String explanation, int limit) {
        return studentQuestions;
    }
}
