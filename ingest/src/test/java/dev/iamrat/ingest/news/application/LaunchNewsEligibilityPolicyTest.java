package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.source.news.application.NewsSourceItem;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LaunchNewsEligibilityPolicyTest {

    private final LaunchNewsEligibilityPolicy policy = new LaunchNewsEligibilityPolicy();

    @Test
    @DisplayName("유효한 출시 뉴스 후보는 허용한다")
    void acceptsValidLaunchNewsCandidate() {
        assertThat(policy.evaluate(candidate(
            "갤럭시북",
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "n.news.naver.com"
        ))).isEmpty();
    }

    @Test
    @DisplayName("광고성 후보는 거절한다")
    void rejectsAdvertisingCandidate() {
        assertThat(policy.evaluate(candidate(
            "갤럭시북",
            "갤럭시북 신제품 특가 이벤트",
            "쿠폰 할인 혜택을 제공한다.",
            "n.news.naver.com"
        ))).contains(LaunchNewsSkipReason.ADVERTISING);
    }

    @Test
    @DisplayName("알 수 없는 출처는 거절한다")
    void rejectsUnknownSource() {
        assertThat(policy.evaluate(candidate(
            "갤럭시북",
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            ""
        ))).contains(LaunchNewsSkipReason.UNKNOWN_SOURCE);
    }

    @Test
    @DisplayName("출시 키워드가 없으면 거절한다")
    void rejectsMissingLaunchKeyword() {
        assertThat(policy.evaluate(candidate(
            "갤럭시북",
            "노트북 시장 점유율 확대",
            "삼성이 노트북 시장에서 점유율을 높였다.",
            "n.news.naver.com"
        ))).contains(LaunchNewsSkipReason.MISSING_LAUNCH_KEYWORD);
    }

    private LaunchNewsCandidate candidate(String keyword, String title, String description, String sourceName) {
        return new LaunchNewsCandidate(
            keyword,
            10L,
            new NewsSourceItem(title, description, "https://n.news.naver.com/article/001/1", "", ""),
            "https://n.news.naver.com/article/001/1",
            "https://n.news.naver.com/article/001/1",
            sourceName,
            LocalDateTime.of(2026, 6, 21, 10, 0)
        );
    }
}
