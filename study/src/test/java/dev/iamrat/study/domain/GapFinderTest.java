package dev.iamrat.study.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GapFinderTest {

    private static final List<String> KEY_POINTS = List.of(
        "문장마다 새 스냅샷을 쓴다",
        "커밋된 데이터만 읽는다",
        "팬텀 리드가 생길 수 있다"
    );

    @Test
    @DisplayName("조사가 달라도 설명에 대부분 나온 핵심 항목은 빠진 것으로 보지 않는다")
    void treatsMostlyMentionedPointsAsCovered() {
        String explanation = "READ COMMITTED에서는 문장마다 스냅샷을 새로 찍고, 커밋된 데이터만 읽어요.";

        assertThat(GapFinder.missing(KEY_POINTS, explanation)).containsExactly("팬텀 리드가 생길 수 있다");
    }

    @Test
    @DisplayName("빈 설명이면 모든 핵심 항목이 빠진 것이다")
    void emptyExplanationMissesEverything() {
        assertThat(GapFinder.missing(KEY_POINTS, "")).isEqualTo(KEY_POINTS);
    }
}
