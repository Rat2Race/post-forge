package dev.iamrat.study.application;

import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.study.domain.KeyPointExtractor;
import java.util.List;

/**
 * LLM이 쓸 만한 질문을 하나도 못 냈을 때의 대체 경로. 핵심 항목을 그대로 근거로 삼는다.
 */
final class RuleQuestionGenerator {

    private RuleQuestionGenerator() {
    }

    static List<QuestionDraft> generate(String content, int limit) {
        return KeyPointExtractor.extract(content).stream()
            .map(point -> new QuestionDraft("'" + point + "'에 대해 설명해 보세요.", point))
            .filter(draft -> StudySourceService.isUsable(content, draft))
            .limit(limit)
            .toList();
    }
}
