package dev.iamrat.source.news.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsSourceItemTest {

    @Test
    @DisplayName("제목이 공백이면 예외가 발생한다")
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> new NewsSourceItem(" ", "설명", "https://news.example.com", "", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("뉴스 제목은 비어 있을 수 없습니다");
    }

    @Test
    @DisplayName("링크가 공백이면 예외가 발생한다")
    void rejectsBlankLink() {
        assertThatThrownBy(() -> new NewsSourceItem("갤럭시북 출시", "설명", " ", "", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("뉴스 링크는 비어 있을 수 없습니다");
    }

    @Test
    @DisplayName("null 필드는 빈 문자열로 정규화한다")
    void normalizesNullFieldsToEmptyString() {
        NewsSourceItem item = new NewsSourceItem(
            "갤럭시북 출시", null, "https://news.example.com", null, null, null, null);

        assertThat(item.description()).isEmpty();
        assertThat(item.originalLink()).isEmpty();
        assertThat(item.publishedAt()).isEmpty();
        assertThat(item.rawTitle()).isEmpty();
        assertThat(item.rawDescription()).isEmpty();
    }
}
