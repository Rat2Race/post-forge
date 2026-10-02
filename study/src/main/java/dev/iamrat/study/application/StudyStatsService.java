package dev.iamrat.study.application;

import dev.iamrat.study.domain.ReviewGrade;
import dev.iamrat.study.domain.ReviewScheduler;
import dev.iamrat.study.domain.StudyQuestionRepository;
import dev.iamrat.study.domain.StudyRecord.Kind;
import dev.iamrat.study.domain.StudyRecordRepository;
import dev.iamrat.study.domain.StudySourceRepository;
import dev.iamrat.study.domain.StudyStreak;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 잔디, 연속 학습일, 학습 지표. ADR-008 게이트 세 수치를 기록에서 바로 계산한다. LLM을 쓰지 않는다.
 * 비율은 표본 수가 함께 보이도록 맞힌 수와 전체 수로 돌려준다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyStatsService {

    // ponytail: 잔디와 연속 학습일을 최근 12주(84일) 기록으로 센다. 그보다 긴 연속은 84에서 멈춘다.
    private static final int HEATMAP_DAYS = 84;
    private static final int GATE_WINDOW_DAYS = 11;
    // 하루·7일 간격 상자. 이 상자에서 한 복습의 '알았음' 비율이 유지율이다.
    private static final int ONE_DAY_BOX = 1;
    private static final int SEVEN_DAY_BOX = 3;
    // 학습 행동만 센다. 꼬리질문은 버튼 하나라 빼고, 그 문제를 푼 답하기가 따로 남는다.
    private static final Set<Kind> STUDY_KINDS = EnumSet.of(Kind.ANSWER, Kind.RECALL, Kind.TEACH, Kind.QUESTION);

    public record DayCount(LocalDate date, int count) {
    }

    public record Rate(int hit, int total) {
    }

    public record StudyStats(
        LocalDate today,
        List<DayCount> days,
        int streak,
        boolean todayDone,
        int activeDaysLast11,
        List<Integer> boxCounts,
        Rate unknown,
        Rate retentionOneDay,
        Rate retentionSevenDay,
        Rate evidencePass
    ) {
    }

    private final StudyRecordRepository recordRepository;
    private final StudyQuestionRepository questionRepository;
    private final StudySourceRepository sourceRepository;
    private final Clock clock;

    public StudyStats of(Long ownerAccountId) {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, Integer> counts = new TreeMap<>();
        recordRepository
            .findActivityTimes(ownerAccountId, today.minusDays(HEATMAP_DAYS - 1L).atStartOfDay(), STUDY_KINDS)
            .forEach(time -> counts.merge(time.toLocalDate(), 1, Integer::sum));
        Set<LocalDate> active = counts.keySet();

        int unknown = 0;
        int answered = 0;
        int[] oneDay = new int[2];
        int[] sevenDay = new int[2];
        for (Object[] row : recordRepository.countAnswersByBoxAndResult(ownerAccountId, Kind.ANSWER)) {
            Integer box = (Integer) row[0];
            boolean good = ReviewGrade.GOOD.name().equals(row[1]);
            int count = ((Number) row[2]).intValue();
            answered += count;
            if (ReviewGrade.AGAIN.name().equals(row[1])) {
                unknown += count;
            }
            if (box != null && box == ONE_DAY_BOX) {
                oneDay[1] += count;
                oneDay[0] += good ? count : 0;
            }
            if (box != null && box == SEVEN_DAY_BOX) {
                sevenDay[1] += count;
                sevenDay[0] += good ? count : 0;
            }
        }

        List<Integer> boxes = new ArrayList<>(Collections.nCopies(ReviewScheduler.boxCount(), 0));
        for (Object[] row : questionRepository.countByBox(ownerAccountId)) {
            boxes.set((Integer) row[0], ((Number) row[1]).intValue());
        }

        Object[] drafts = sourceRepository.sumDraftedAndDiscarded(ownerAccountId).get(0);
        int drafted = ((Number) drafts[0]).intValue();
        int discarded = ((Number) drafts[1]).intValue();

        return new StudyStats(
            today,
            counts.entrySet().stream().map(entry -> new DayCount(entry.getKey(), entry.getValue())).toList(),
            StudyStreak.of(active, today),
            active.contains(today),
            StudyStreak.activeDays(active, today, GATE_WINDOW_DAYS),
            boxes,
            new Rate(unknown, answered),
            new Rate(oneDay[0], oneDay[1]),
            new Rate(sevenDay[0], sevenDay[1]),
            new Rate(drafted - discarded, drafted)
        );
    }
}
