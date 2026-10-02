package dev.iamrat.study.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecallGradeTest {

    @Test
    @DisplayName("핵심 항목의 80% 이상을 떠올리면 알았음, 50% 이상이면 애매함, 그 아래면 몰랐음으로 일정을 정한다")
    void mapsRecallRatioToGrade() {
        assertThat(RecallGrade.of(4, 5)).isEqualTo(ReviewGrade.GOOD);
        assertThat(RecallGrade.of(3, 5)).isEqualTo(ReviewGrade.HARD);
        assertThat(RecallGrade.of(5, 10)).isEqualTo(ReviewGrade.HARD);
        assertThat(RecallGrade.of(2, 5)).isEqualTo(ReviewGrade.AGAIN);
    }

    @Test
    @DisplayName("핵심 항목이 없으면 잴 수 없으니 같은 칸에 둔다")
    void noKeyPointsKeepsTheBox() {
        assertThat(RecallGrade.of(0, 0)).isEqualTo(ReviewGrade.HARD);
    }
}
