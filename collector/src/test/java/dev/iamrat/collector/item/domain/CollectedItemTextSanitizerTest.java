package dev.iamrat.collector.item.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectedItemTextSanitizerTest {

    private final CollectedItemTextSanitizer collectedItemTextSanitizer = new CollectedItemTextSanitizer();

    @Test
    void sanitize_removesHtmlTagsAndDecodesKnownEntities() {
        String sanitized = collectedItemTextSanitizer.sanitize("<b>AI &amp; 반도체</b> &lt;성장&gt;");

        assertThat(sanitized).isEqualTo("AI & 반도체 <성장>");
    }

    @Test
    void sanitize_removesNumericAndUnknownNamedEntities() {
        String sanitized = collectedItemTextSanitizer.sanitize("뉴스&#123;본문&nbsp;요약");

        assertThat(sanitized).isEqualTo("뉴스본문 요약");
    }

    @Test
    void sanitize_nullText_returnsEmptyString() {
        assertThat(collectedItemTextSanitizer.sanitize(null)).isEmpty();
    }
}
