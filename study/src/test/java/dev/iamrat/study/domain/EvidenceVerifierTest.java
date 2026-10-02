package dev.iamrat.study.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvidenceVerifierTest {

    private static final String CONTENT = """
        Redis는 조회수를 모아 두었다가
        5분마다   DB에 반영한다.
        """;

    @Test
    @DisplayName("줄바꿈과 공백이 달라도 원문에 있는 구절이면 근거로 인정한다")
    void acceptsQuoteThatExistsDespiteWhitespace() {
        assertThat(EvidenceVerifier.isQuoted(CONTENT, "조회수를 모아 두었다가 5분마다 DB에 반영한다")).isTrue();
    }

    @Test
    @DisplayName("웹에서 붙여 넣은 줄바꿈 없는 공백·전각 공백이 있어도 원문 구절이면 근거로 인정한다")
    void acceptsQuoteDespiteNonBreakingAndIdeographicSpaces() {
        String pasted = "커밋된 데이터만　읽는다";

        assertThat(EvidenceVerifier.isQuoted(pasted, "커밋된 데이터만 읽는다")).isTrue();
    }

    @Test
    @DisplayName("자모가 분해된(NFD) 한글 자료에서도 같은 글자로 쓴 근거를 인정한다")
    void acceptsQuoteWhenSourceHangulIsDecomposed() {
        String decomposed = java.text.Normalizer.normalize("커밋된 데이터만 읽는다", java.text.Normalizer.Form.NFD);

        assertThat(EvidenceVerifier.isQuoted(decomposed, "커밋된 데이터만 읽는다")).isTrue();
    }

    @Test
    @DisplayName("원문에 없는 바꿔 쓴 문장은 근거로 인정하지 않는다")
    void rejectsParaphrase() {
        assertThat(EvidenceVerifier.isQuoted(CONTENT, "조회수는 Redis에 저장된다")).isFalse();
    }

    @Test
    @DisplayName("너무 짧은 구절은 아무 데나 걸리므로 근거로 인정하지 않는다")
    void rejectsTooShortQuote() {
        assertThat(EvidenceVerifier.isQuoted(CONTENT, "Redis")).isFalse();
        assertThat(EvidenceVerifier.isQuoted(CONTENT, null)).isFalse();
    }
}
