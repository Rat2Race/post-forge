package dev.iamrat.study.domain;

import java.time.LocalDate;
import java.util.Set;

/** 학습한 날짜로 연속 학습일과 기간 내 사용일을 센다. 날짜는 Asia/Seoul 기준이다. */
public final class StudyStreak {

    private StudyStreak() {
    }

    /** 오늘을 아직 안 했으면 어제부터 센다. 오늘 미완료는 끊김이 아니다. */
    public static int of(Set<LocalDate> activeDays, LocalDate today) {
        LocalDate day = activeDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (activeDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    /** 오늘을 포함한 최근 window일 중 학습한 날 수. ADR-008 게이트 '11일 중 8일'의 분자다. */
    public static int activeDays(Set<LocalDate> activeDays, LocalDate today, int window) {
        LocalDate from = today.minusDays(window - 1L);
        return (int) activeDays.stream().filter(day -> !day.isBefore(from) && !day.isAfter(today)).count();
    }
}
