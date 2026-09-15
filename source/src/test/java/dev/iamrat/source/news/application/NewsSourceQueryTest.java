package dev.iamrat.source.news.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsSourceQueryTest {

    @Test
    @DisplayName("키워드가 null이거나 공백이면 예외가 발생한다")
    void rejectsNullOrBlankKeyword() {
        assertThatThrownBy(() -> new NewsSourceQuery(null, 10, "date"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("뉴스 키워드는 비어 있을 수 없습니다");
        assertThatThrownBy(() -> new NewsSourceQuery("  ", 10, "date"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("뉴스 키워드는 비어 있을 수 없습니다");
    }

    @Test
    @DisplayName("displayCount가 0 이하이면 10으로 보정한다")
    void defaultsDisplayCountToTenWhenNotPositive() {
        assertThat(new NewsSourceQuery("갤럭시북", 0, "date").displayCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("displayCount가 100을 넘으면 100으로 제한한다")
    void clampsDisplayCountToHundred() {
        assertThat(new NewsSourceQuery("갤럭시북", 101, "date").displayCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("sort가 null이거나 공백이면 date를 기본값으로 사용한다")
    void defaultsSortToDateWhenNullOrBlank() {
        assertThat(new NewsSourceQuery("갤럭시북", 10, null).sort()).isEqualTo("date");
        assertThat(new NewsSourceQuery("갤럭시북", 10, " ").sort()).isEqualTo("date");
    }

    @ParameterizedTest
    @ValueSource(strings = {"accuracy", "DATE", "unknown"})
    @DisplayName("지원하지 않는 sort는 거부한다")
    void rejectsUnsupportedSort(String sort) {
        assertThatThrownBy(() -> new NewsSourceQuery("갤럭시북", 10, sort))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("뉴스 정렬은 date 또는 sim이어야 합니다");
    }
}
