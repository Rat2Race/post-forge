package dev.iamrat.core.study;

import java.util.List;

/**
 * 학습 활동에서 LLM이 맡는 일. 묻거나 제안만 하고 채점하지 않는다.
 * 실패하면 빈 목록을 돌려주고, 근거 검증과 대체 경로는 호출하는 쪽이 맡는다.
 */
public interface StudyAssistant {

    List<QuestionDraft> draftQuestions(String content, int limit);

    List<String> askAsStudent(String content, String explanation, int limit);

    /** 앞 문제를 한 단계 더 파고드는 꼬리질문. context는 앞 문제 근거 주변의 자료 일부다. */
    List<QuestionDraft> draftFollowUps(String context, String question, String evidence, int limit);
}
