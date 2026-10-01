package dev.iamrat.study.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.study.domain.EvidenceVerifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleQuestionGeneratorTest {

    private static final String CONTENT = """
        # 트랜잭션 격리 수준
        - 커밋된 데이터만 읽는다
        - 문장마다 새 스냅샷을 쓴다
        """;

    @Test
    @DisplayName("핵심 항목마다 설명을 요구하는 질문을 만들고, 그 항목 문장을 근거로 단다")
    void asksToExplainEachKeyPointWithItsLineAsEvidence() {
        List<QuestionDraft> drafts = RuleQuestionGenerator.generate(CONTENT, 2);

        assertThat(drafts).containsExactly(
            new QuestionDraft("'트랜잭션 격리 수준'에 대해 설명해 보세요.", "트랜잭션 격리 수준"),
            new QuestionDraft("'커밋된 데이터만 읽는다'에 대해 설명해 보세요.", "커밋된 데이터만 읽는다")
        );
    }

    @Test
    @DisplayName("규칙으로 만든 질문의 근거는 언제나 원문 검증을 통과한다")
    void evidenceAlwaysPassesVerification() {
        assertThat(RuleQuestionGenerator.generate(CONTENT, 10))
            .hasSize(3)
            .allSatisfy(draft -> assertThat(EvidenceVerifier.isQuoted(CONTENT, draft.evidence())).isTrue());
    }
}
