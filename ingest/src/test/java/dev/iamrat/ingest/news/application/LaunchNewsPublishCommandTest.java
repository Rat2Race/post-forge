package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LaunchNewsPublishCommandTest {

    @Test
    @DisplayName("category가 null이면 GENERAL로 정규화한다")
    void normalizesNullCategoryToGeneral() {
        LaunchNewsPublishCommand command = new LaunchNewsPublishCommand(
            "갤럭시북",
            5,
            3,
            List.of("출시"),
            null,
            PostPublishOrigin.SYSTEM_BATCH
        );

        assertThat(command.category()).isEqualTo(BoardCategory.GENERAL);
    }

    @Test
    @DisplayName("keyword가 null이거나 공백이면 예외를 던진다")
    void rejectsNullOrBlankKeyword() {
        assertThatThrownBy(() -> command(null, 5, 3, List.of(), PostPublishOrigin.SYSTEM_BATCH))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> command("  ", 5, 3, List.of(), PostPublishOrigin.SYSTEM_BATCH))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("keyword 앞뒤 공백을 제거한다")
    void trimsKeyword() {
        LaunchNewsPublishCommand command = command("  갤럭시북  ", 5, 3, List.of(), PostPublishOrigin.SYSTEM_BATCH);

        assertThat(command.keyword()).isEqualTo("갤럭시북");
    }

    @Test
    @DisplayName("displayCount가 null이면 10으로 정규화한다")
    void normalizesNullDisplayCountToTen() {
        LaunchNewsPublishCommand command = command("갤럭시북", null, 3, List.of(), PostPublishOrigin.SYSTEM_BATCH);

        assertThat(command.displayCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("displayCount를 1..100 범위로 자른다")
    void clampsDisplayCountBetweenOneAndOneHundred() {
        assertThat(command("갤럭시북", 0, 3, List.of(), PostPublishOrigin.SYSTEM_BATCH).displayCount())
            .isEqualTo(1);
        assertThat(command("갤럭시북", 101, 3, List.of(), PostPublishOrigin.SYSTEM_BATCH).displayCount())
            .isEqualTo(100);
    }

    @Test
    @DisplayName("dailyCap이 null이면 3으로 정규화한다")
    void normalizesNullDailyCapToThree() {
        LaunchNewsPublishCommand command = command("갤럭시북", 5, null, List.of(), PostPublishOrigin.SYSTEM_BATCH);

        assertThat(command.dailyCap()).isEqualTo(3);
    }

    @Test
    @DisplayName("dailyCap을 1..20 범위로 자른다")
    void clampsDailyCapBetweenOneAndTwenty() {
        assertThat(command("갤럭시북", 5, 0, List.of(), PostPublishOrigin.SYSTEM_BATCH).dailyCap())
            .isEqualTo(1);
        assertThat(command("갤럭시북", 5, 25, List.of(), PostPublishOrigin.SYSTEM_BATCH).dailyCap())
            .isEqualTo(20);
    }

    @Test
    @DisplayName("topics가 null이면 빈 리스트로 정규화한다")
    void normalizesNullTopicsToEmptyList() {
        LaunchNewsPublishCommand command = command("갤럭시북", 5, 3, null, PostPublishOrigin.SYSTEM_BATCH);

        assertThat(command.topics()).isEmpty();
    }

    @Test
    @DisplayName("topics의 공백 항목을 거르고 trim·중복 제거 후 10개까지만 남긴다")
    void filtersTrimsDeduplicatesAndLimitsTopics() {
        List<String> topics = Arrays.asList(
            " 출시 ", "출시", null, "  ",
            "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"
        );

        LaunchNewsPublishCommand command = command("갤럭시북", 5, 3, topics, PostPublishOrigin.SYSTEM_BATCH);

        assertThat(command.topics())
            .containsExactly("출시", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9");
    }

    @Test
    @DisplayName("publishOrigin이 ADMIN_BACKFILL이 아니면 SYSTEM_BATCH로 강제한다")
    void forcesNonBackfillPublishOriginToSystemBatch() {
        assertThat(command("갤럭시북", 5, 3, List.of(), null).publishOrigin())
            .isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
        assertThat(command("갤럭시북", 5, 3, List.of(), PostPublishOrigin.USER).publishOrigin())
            .isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("publishOrigin이 ADMIN_BACKFILL이면 그대로 유지한다")
    void keepsAdminBackfillPublishOrigin() {
        assertThat(command("갤럭시북", 5, 3, List.of(), PostPublishOrigin.ADMIN_BACKFILL).publishOrigin())
            .isEqualTo(PostPublishOrigin.ADMIN_BACKFILL);
    }

    private LaunchNewsPublishCommand command(
        String keyword,
        Integer displayCount,
        Integer dailyCap,
        List<String> topics,
        PostPublishOrigin publishOrigin
    ) {
        return new LaunchNewsPublishCommand(
            keyword,
            displayCount,
            dailyCap,
            topics,
            BoardCategory.GENERAL,
            publishOrigin
        );
    }
}
