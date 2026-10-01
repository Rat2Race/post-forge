package dev.iamrat.study.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Test
    @DisplayName("알았으면 한 칸 올라가 그 칸의 간격 뒤에 다시 나온다")
    void goodMovesUpOneBox() {
        assertThat(ReviewScheduler.next(0, ReviewGrade.GOOD, NOW))
            .isEqualTo(new ReviewScheduler.Next(1, LocalDateTime.of(2026, 10, 2, 9, 0)));
        assertThat(ReviewScheduler.next(2, ReviewGrade.GOOD, NOW))
            .isEqualTo(new ReviewScheduler.Next(3, LocalDateTime.of(2026, 10, 8, 9, 0)));
    }

    @Test
    @DisplayName("맨 위 칸에서 알았으면 그 칸에 머물고 30일 뒤에 나온다")
    void goodStaysAtTopBox() {
        assertThat(ReviewScheduler.next(5, ReviewGrade.GOOD, NOW))
            .isEqualTo(new ReviewScheduler.Next(5, LocalDateTime.of(2026, 10, 31, 9, 0)));
    }

    @Test
    @DisplayName("애매했으면 같은 칸에 머물고 그 칸의 간격 뒤에 다시 나온다")
    void hardStaysInSameBox() {
        assertThat(ReviewScheduler.next(3, ReviewGrade.HARD, NOW))
            .isEqualTo(new ReviewScheduler.Next(3, LocalDateTime.of(2026, 10, 8, 9, 0)));
    }

    @Test
    @DisplayName("몰랐으면 처음 칸으로 돌아가 10분 뒤에 다시 나온다")
    void againResetsToFirstBox() {
        assertThat(ReviewScheduler.next(4, ReviewGrade.AGAIN, NOW))
            .isEqualTo(new ReviewScheduler.Next(0, LocalDateTime.of(2026, 10, 1, 9, 10)));
    }
}
