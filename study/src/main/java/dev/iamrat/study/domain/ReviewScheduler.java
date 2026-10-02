package dev.iamrat.study.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 라이트너 상자 방식 간격 반복. 알면 한 칸 위로, 애매하면 제자리, 모르면 첫 칸.
 * ponytail: 고정 간격표. 개인별 망각 곡선(SM-2·FSRS)은 복습 기록이 쌓인 뒤에 바꾼다.
 */
public final class ReviewScheduler {

    private static final List<Duration> INTERVALS = List.of(
        Duration.ofMinutes(10),
        Duration.ofDays(1),
        Duration.ofDays(3),
        Duration.ofDays(7),
        Duration.ofDays(14),
        Duration.ofDays(30)
    );
    // 빈 페이지는 몇 분짜리 글쓰기라 10분 단계가 없다.
    private static final List<Duration> RECALL_INTERVALS = List.of(
        Duration.ofDays(1),
        Duration.ofDays(3),
        Duration.ofDays(7),
        Duration.ofDays(14),
        Duration.ofDays(30)
    );

    public record Next(int box, LocalDateTime dueAt) {
    }

    private ReviewScheduler() {
    }

    public static Next next(int box, ReviewGrade grade, LocalDateTime now) {
        return step(INTERVALS, box, grade, now);
    }

    public static Next nextRecall(int box, ReviewGrade grade, LocalDateTime now) {
        return step(RECALL_INTERVALS, box, grade, now);
    }

    public static LocalDateTime firstRecallAt(LocalDateTime now) {
        return now.plus(RECALL_INTERVALS.get(0));
    }

    private static Next step(List<Duration> intervals, int box, ReviewGrade grade, LocalDateTime now) {
        int top = intervals.size() - 1;
        int nextBox = switch (grade) {
            case AGAIN -> 0;
            case HARD -> Math.min(box, top);
            case GOOD -> Math.min(box + 1, top);
        };
        return new Next(nextBox, now.plus(intervals.get(nextBox)));
    }
}
