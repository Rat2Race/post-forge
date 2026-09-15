package dev.iamrat.core.board.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DailyDigestSourceItemTest {

    @Test
    @DisplayName("summary가 null이면 빈 문자열로 정규화한다")
    void summary_nullIsNormalizedToEmptyString() {
        assertThat(new DailyDigestSourceItem("갤럭시북 출시", null).summary()).isEmpty();
        assertThat(new DailyDigestSourceItem("갤럭시북 출시", "요약").summary()).isEqualTo("요약");
    }
}
