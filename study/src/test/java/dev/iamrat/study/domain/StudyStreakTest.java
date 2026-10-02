package dev.iamrat.study.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StudyStreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    @Test
    @DisplayName("오늘까지 이어진 날 수를 센다")
    void countsConsecutiveDaysEndingToday() {
        assertThat(StudyStreak.of(Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(3);
    }

    @Test
    @DisplayName("오늘을 아직 안 했으면 어제까지 이어진 날로 센다 — 오늘 미완료는 끊김이 아니다")
    void unfinishedTodayDoesNotBreakTheStreak() {
        assertThat(StudyStreak.of(Set.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(2);
    }

    @Test
    @DisplayName("어제도 오늘도 안 했으면 0이다")
    void zeroWhenYesterdayAndTodayAreEmpty() {
        assertThat(StudyStreak.of(Set.of(TODAY.minusDays(2), TODAY.minusDays(3)), TODAY)).isZero();
    }

    @Test
    @DisplayName("하루라도 비면 그 앞은 세지 않는다")
    void stopsAtTheFirstGap() {
        assertThat(StudyStreak.of(Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(3)), TODAY)).isEqualTo(2);
    }

    @Test
    @DisplayName("최근 N일 중 활동한 날 수를 센다 — 오늘을 포함한다")
    void countsActiveDaysInWindow() {
        assertThat(StudyStreak.activeDays(Set.of(TODAY, TODAY.minusDays(5), TODAY.minusDays(10), TODAY.minusDays(11)), TODAY, 11))
            .isEqualTo(3);
    }
}
