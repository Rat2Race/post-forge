package dev.iamrat.study.application;

import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.core.study.StudyAssistant;
import java.util.List;

/** LLM 대신 테스트가 정한 답을 돌려준다. 빈 목록은 LLM 장애와 같다. */
class FakeStudyAssistant implements StudyAssistant {

    List<QuestionDraft> drafts = List.of();
    List<String> studentQuestions = List.of();
    List<QuestionDraft> followUps = List.of();
    String lastFollowUpContext;
    // 매일 반복 루프에 LLM 호출이 섞이지 않았는지 세기 위한 수.
    int calls;

    @Override
    public List<QuestionDraft> draftQuestions(String content, int limit) {
        calls++;
        return drafts;
    }

    @Override
    public List<String> askAsStudent(String content, String explanation, int limit) {
        calls++;
        return studentQuestions;
    }

    @Override
    public List<QuestionDraft> draftFollowUps(String context, String question, String evidence, int limit) {
        calls++;
        lastFollowUpContext = context;
        return followUps;
    }
}
