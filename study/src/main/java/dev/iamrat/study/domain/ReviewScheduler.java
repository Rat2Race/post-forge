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
    private static final int TOP_BOX = INTERVALS.size() - 1;

    public record Next(int box, LocalDateTime dueAt) {
    }

    private ReviewScheduler() {
    }

    public static Next next(int box, ReviewGrade grade, LocalDateTime now) {
        int nextBox = switch (grade) {
            case AGAIN -> 0;
            case HARD -> box;
            case GOOD -> Math.min(box + 1, TOP_BOX);
        };
        return new Next(nextBox, now.plus(INTERVALS.get(nextBox)));
    }
}
