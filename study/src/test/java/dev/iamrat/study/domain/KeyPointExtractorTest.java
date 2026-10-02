package dev.iamrat.study.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeyPointExtractorTest {

    @Test
    @DisplayName("마크다운 제목과 목록 항목을 표식 없이 순서대로 뽑는다")
    void extractsHeadingsAndListItemsInOrder() {
        String content = """
            # 트랜잭션 격리 수준
            본문 설명 문장입니다.
            ## READ COMMITTED
            - 문장마다 새 스냅샷을 쓴다
            * 커밋된 데이터만 읽는다
            1. 첫 번째 항목
            """;

        assertThat(KeyPointExtractor.extract(content)).containsExactly(
            "트랜잭션 격리 수준",
            "READ COMMITTED",
            "문장마다 새 스냅샷을 쓴다",
            "커밋된 데이터만 읽는다",
            "첫 번째 항목"
        );
    }

    @Test
    @DisplayName("제목과 목록이 없으면 문단마다 첫 문장을 뽑는다")
    void fallsBackToFirstSentenceOfEachParagraph() {
        String content = "첫 문장이다. 둘째 문장.\n\n다음 문단 첫 문장! 그리고 더.";

        assertThat(KeyPointExtractor.extract(content)).containsExactly("첫 문장이다.", "다음 문단 첫 문장!");
    }

    @Test
    @DisplayName("코드 블록 안의 # 주석과 - 항목은 핵심 항목으로 보지 않는다")
    void ignoresMarkersInsideCodeFence() {
        String content = """
            ## 설치
            ```bash
            # 의존성을 받는다
            - not a bullet
            ```
            - 실행 전에 환경변수를 넣는다
            """;

        assertThat(KeyPointExtractor.extract(content)).containsExactly("설치", "실행 전에 환경변수를 넣는다");
    }

    @Test
    @DisplayName("같은 항목은 한 번만, 최대 20개까지만 뽑는다")
    void dedupesAndCapsAtTwenty() {
        String repeated = "- 같은 항목\n- 같은 항목\n";
        String many = IntStream.rangeClosed(1, 25)
            .mapToObj(i -> "- 항목 " + i)
            .collect(Collectors.joining("\n"));

        assertThat(KeyPointExtractor.extract(repeated)).containsExactly("같은 항목");
        assertThat(KeyPointExtractor.extract(many)).hasSize(20).startsWith("항목 1").endsWith("항목 20");
    }
}
