package dev.iamrat.study.domain;

/**
 * 빈 페이지 결과를 다음 일정으로 바꾼다. 채점이 아니라 사용자가 직접 체크한 핵심 항목의 비율이다.
 * 80% 이상 떠올리면 한 칸 위, 50% 이상이면 제자리, 그 아래면 첫 칸.
 */
public final class RecallGrade {

    private RecallGrade() {
    }

    public static ReviewGrade of(int recalled, int total) {
        if (total <= 0) {
            return ReviewGrade.HARD;
        }
        // 비율을 정수로 비교해 소수점 오차를 피한다: recalled / total >= 0.8  ⇔  5 * recalled >= 4 * total
        if (5 * recalled >= 4 * total) {
            return ReviewGrade.GOOD;
        }
        return 2 * recalled >= total ? ReviewGrade.HARD : ReviewGrade.AGAIN;
    }
}
