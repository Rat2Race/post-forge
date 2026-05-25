package dev.iamrat.ai.generation.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationPolicyTest {

    private final GenerationPolicy generationPolicy = new GenerationPolicy();

    @Test
    void sanitizeText_replacesBannedPhrases() {
        String sanitized = generationPolicy.sanitizeText("확정적 상승, 수익 보장");

        assertThat(sanitized)
            .doesNotContain("확정적 상승")
            .doesNotContain("수익 보장")
            .contains("가능성 및 리스크를 함께 확인해야 합니다");
    }

    @Test
    void sanitizeText_blankText_returnsInput() {
        assertThat(generationPolicy.sanitizeText(" ")).isEqualTo(" ");
        assertThat(generationPolicy.sanitizeText(null)).isNull();
    }

    @Test
    void appendDisclaimer_addsFactCheckNotice() {
        String content = generationPolicy.appendDisclaimer("본문");

        assertThat(content)
            .startsWith("본문")
            .contains("AI가 자동 생성한 트렌드 분석입니다")
            .contains("사실 관계와 맥락을 다시 확인하세요");
    }

    @Test
    void appendDisclaimer_doesNotDuplicateExistingNotice() {
        String content = "본문\n사실 관계와 맥락을 다시 확인하세요";

        assertThat(generationPolicy.appendDisclaimer(content)).isEqualTo(content);
    }

    @Test
    void appendDisclaimer_blankContent_returnsNoticeOnly() {
        assertThat(generationPolicy.appendDisclaimer(" "))
            .startsWith("---")
            .contains("AI가 자동 생성한 트렌드 분석입니다")
            .contains("사실 관계와 맥락을 다시 확인하세요");
    }
}
