package dev.iamrat.ingest.pipeline.domain;

import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngestPolicyTest {

    private final IngestPolicy ingestPolicy = new IngestPolicy();

    @Test
    void autoPostMetadata_autoPostEligibleNews_returnsMetadata() {
        Optional<NewsDocumentMetadata> metadata = ingestPolicy.autoPostMetadata(
            NewsDocumentMetadata.SOURCE_NAVER_NEWS,
            NewsDocumentMetadata.autoPostEligible(
                "AI",
                "AI 반도체 수요 증가",
                "https://news.example/1",
                ""
            ).toMap()
        );

        assertThat(metadata)
            .hasValueSatisfying(value -> {
                assertThat(value.keyword()).isEqualTo("AI");
                assertThat(value.originalLink()).isEqualTo("https://news.example/1");
            });
    }

    @Test
    void autoPostMetadata_nonNewsSource_returnsEmpty() {
        Optional<NewsDocumentMetadata> metadata = ingestPolicy.autoPostMetadata(
            "manual",
            NewsDocumentMetadata.autoPostEligible(
                "AI",
                "AI 반도체 수요 증가",
                "https://news.example/1",
                ""
            ).toMap()
        );

        assertThat(metadata).isEmpty();
    }

    @Test
    void autoPostMetadata_incompleteMetadata_returnsEmpty() {
        Optional<NewsDocumentMetadata> metadata = ingestPolicy.autoPostMetadata(
            NewsDocumentMetadata.SOURCE_NAVER_NEWS,
            new NewsDocumentMetadata(
                "AI",
                "AI 반도체 수요 증가",
                "",
                "",
                true
            ).toMap()
        );

        assertThat(metadata).isEmpty();
    }

    @Test
    void reserveAutoPostOriginalLink_allowsOnlyFirstLink() {
        NewsDocumentMetadata metadata = NewsDocumentMetadata.autoPostEligible(
            "AI",
            "AI 반도체 수요 증가",
            "https://news.example/1",
            ""
        );
        Set<String> reservedLinks = new LinkedHashSet<>();

        assertThat(ingestPolicy.reserveAutoPostOriginalLink(metadata, reservedLinks)).isTrue();
        assertThat(ingestPolicy.reserveAutoPostOriginalLink(metadata, reservedLinks)).isFalse();
    }
}
