package dev.iamrat.study.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.study.domain.TodayPlan.Due;
import dev.iamrat.study.domain.TodayPlan.Kind;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TodayPlanTest {

    private static final LocalDateTime DAY = LocalDateTime.of(2026, 10, 2, 0, 0);
    private static final long A = 1, B = 2, C = 3;

    private static Due question(long id, long sourceId, int hour) {
        return new Due(Kind.QUESTION, id, sourceId, DAY.plusHours(hour));
    }

    private static Due recall(long sourceId, LocalDateTime dueAt) {
        return new Due(Kind.RECALL, sourceId, sourceId, dueAt);
    }

    private static final List<Due> QUESTIONS = List.of(question(11, A, 8), question(12, B, 7), question(13, A, 9), question(14, C, 6));
    private static final List<Due> RECALLS = List.of(recall(A, DAY.minusHours(14)), recall(B, DAY.plusHours(5)));

    @Test
    @DisplayName("빈 페이지는 같은 자료의 문제보다 먼저 나오고, 묶음은 가장 이른 예정 시각 순이다")
    void recallComesBeforeItsSourcesQuestions() {
        TodayPlan.Plan plan = TodayPlan.of(QUESTIONS, RECALLS, 20, 3);

        assertThat(plan.items()).extracting(Due::kind, Due::id).containsExactly(
            org.assertj.core.groups.Tuple.tuple(Kind.RECALL, A),
            org.assertj.core.groups.Tuple.tuple(Kind.QUESTION, 11L),
            org.assertj.core.groups.Tuple.tuple(Kind.QUESTION, 13L),
            org.assertj.core.groups.Tuple.tuple(Kind.RECALL, B),
            org.assertj.core.groups.Tuple.tuple(Kind.QUESTION, 12L),
            org.assertj.core.groups.Tuple.tuple(Kind.QUESTION, 14L));
        assertThat(plan.remaining()).isZero();
    }

    @Test
    @DisplayName("빈 페이지는 하루 상한까지만 넣고, 빠진 자료의 문제는 따로 시각 순으로 둔다")
    void capsRecallsPerDay() {
        TodayPlan.Plan plan = TodayPlan.of(QUESTIONS, RECALLS, 20, 1);

        assertThat(plan.items()).extracting(Due::id).containsExactly(A, 11L, 13L, 14L, 12L);
        assertThat(plan.remaining()).isEqualTo(1);
    }

    @Test
    @DisplayName("전체 상한을 넘는 것은 자르고 남은 수를 알려 준다")
    void capsTotalAndCountsTheRest() {
        TodayPlan.Plan plan = TodayPlan.of(QUESTIONS, RECALLS, 3, 3);

        assertThat(plan.items()).extracting(Due::id).containsExactly(A, 11L, 13L);
        assertThat(plan.remaining()).isEqualTo(3);
    }
}
