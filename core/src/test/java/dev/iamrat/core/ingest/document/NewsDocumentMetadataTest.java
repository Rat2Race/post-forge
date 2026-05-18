package dev.iamrat.core.ingest.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsDocumentMetadataTest {

    @Test
    @DisplayName("뉴스 자동 게시 metadata를 map 계약으로 직렬화하고 복원한다")
    void toMapAndFrom_roundTripsNewsMetadata() {
        NewsDocumentMetadata metadata = NewsDocumentMetadata.autoPostEligible(
            "테크",
            "AI 반도체 수요 증가",
            "https://news.example/1",
            "Mon, 15 Mar 2026 09:00:00 +0900"
        );

        Map<String, String> payload = metadata.toMap();

        assertThat(payload)
            .containsEntry(NewsDocumentMetadata.KEYWORD, "테크")
            .containsEntry(NewsDocumentMetadata.NEWS_TITLE, "AI 반도체 수요 증가")
            .containsEntry(NewsDocumentMetadata.ORIGINAL_LINK, "https://news.example/1")
            .containsEntry(NewsDocumentMetadata.PUBLISHED_AT, "Mon, 15 Mar 2026 09:00:00 +0900")
            .containsEntry(NewsDocumentMetadata.AUTO_POST_ELIGIBLE, "true");
        assertThat(NewsDocumentMetadata.from(NewsDocumentMetadata.SOURCE_NAVER_NEWS, payload))
            .contains(metadata);
    }

    @Test
    @DisplayName("뉴스 source가 아니면 metadata 계약으로 해석하지 않는다")
    void from_nonNewsSource_returnsEmpty() {
        Map<String, String> payload = NewsDocumentMetadata.autoPostEligible(
            "테크",
            "AI 반도체 수요 증가",
            "https://news.example/1",
            ""
        ).toMap();

        assertThat(NewsDocumentMetadata.from("other-source", payload)).isEmpty();
    }

    @Test
    @DisplayName("자동 게시에는 opt-in과 필수 뉴스 필드가 모두 필요하다")
    void canPublishAutomatically_requiresOptInAndRequiredFields() {
        assertThat(NewsDocumentMetadata.autoPostEligible(
            "테크",
            "AI 반도체 수요 증가",
            "https://news.example/1",
            ""
        ).canPublishAutomatically()).isTrue();

        assertThat(new NewsDocumentMetadata(
            "테크",
            "AI 반도체 수요 증가",
            "https://news.example/1",
            "",
            false
        ).canPublishAutomatically()).isFalse();

        assertThat(NewsDocumentMetadata.autoPostEligible(
            "테크",
            " ",
            "https://news.example/1",
            ""
        ).canPublishAutomatically()).isFalse();
    }
}
