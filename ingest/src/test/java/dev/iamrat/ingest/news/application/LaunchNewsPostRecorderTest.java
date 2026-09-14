package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceLinkCommand;
import dev.iamrat.core.board.post.PostReferenceLinkWriter;
import dev.iamrat.core.board.post.PostReferenceProvider;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import dev.iamrat.source.news.application.NewsSourceItem;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaunchNewsPostRecorderTest {

    @Mock
    private PostWriter postWriter;

    @Mock
    private PostReferenceLinkWriter referenceLinkWriter;

    private LaunchNewsPostRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new LaunchNewsPostRecorder(
            postWriter,
            referenceLinkWriter,
            Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    @DisplayName("시스템 계정으로 게시글을 쓰고 반환된 id로 참조 링크를 저장한다")
    void writesPostAsSystemAccountAndReferenceLinkWithReturnedId() {
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        Long postId = recorder.record(draft(), candidate(), PostPublishOrigin.SYSTEM_BATCH);

        assertThat(postId).isEqualTo(42L);

        ArgumentCaptor<PostWriteCommand> postCommand = ArgumentCaptor.forClass(PostWriteCommand.class);
        verify(postWriter).write(postCommand.capture());
        assertThat(postCommand.getValue().title()).isEqualTo("갤럭시북 신제품 출시");
        assertThat(postCommand.getValue().accountId()).isEqualTo(0L);
        assertThat(postCommand.getValue().nickname()).isEqualTo("PostForge News Bot");
        assertThat(postCommand.getValue().category()).isEqualTo(PostCategory.PRODUCT_LAUNCH_NEWS);
        assertThat(postCommand.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);

        ArgumentCaptor<PostReferenceLinkCommand> referenceCommand =
            ArgumentCaptor.forClass(PostReferenceLinkCommand.class);
        verify(referenceLinkWriter).write(referenceCommand.capture());
        assertThat(referenceCommand.getValue().postId()).isEqualTo(42L);
        assertThat(referenceCommand.getValue().keyword()).isEqualTo("갤럭시북");
        assertThat(referenceCommand.getValue().provider()).isEqualTo(PostReferenceProvider.NAVER_NEWS);
        assertThat(referenceCommand.getValue().canonicalUrl())
            .isEqualTo("https://n.news.naver.com/article/001/1");
        assertThat(referenceCommand.getValue().publishedAt())
            .isEqualTo(LocalDateTime.of(2026, 6, 21, 10, 0));
        assertThat(referenceCommand.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("원본 발행 시간이 없으면 처리 시간을 참조 링크에 저장한다")
    void storesProcessingTimeWhenPublishedAtIsMissing() {
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        recorder.record(draft(), candidateWithoutPublishedAt(), PostPublishOrigin.SYSTEM_BATCH);

        ArgumentCaptor<PostReferenceLinkCommand> referenceCommand =
            ArgumentCaptor.forClass(PostReferenceLinkCommand.class);
        verify(referenceLinkWriter).write(referenceCommand.capture());
        assertThat(referenceCommand.getValue().publishedAt())
            .isEqualTo(LocalDateTime.of(2026, 6, 21, 9, 0));
    }

    private LaunchNewsPostDraft draft() {
        return new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"));
    }

    private LaunchNewsCandidate candidate() {
        return new LaunchNewsCandidate(
            "갤럭시북",
            item(),
            "https://n.news.naver.com/article/001/1",
            "https://n.news.naver.com/article/001/1?utm=1",
            "n.news.naver.com",
            LocalDateTime.of(2026, 6, 21, 10, 0),
            BoardCategory.GENERAL
        );
    }

    private LaunchNewsCandidate candidateWithoutPublishedAt() {
        return new LaunchNewsCandidate(
            "갤럭시북",
            item(),
            "https://n.news.naver.com/article/001/1",
            "https://n.news.naver.com/article/001/1?utm=1",
            "n.news.naver.com",
            null,
            BoardCategory.GENERAL
        );
    }

    private NewsSourceItem item() {
        return new NewsSourceItem(
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "https://n.news.naver.com/article/001/1?ntype=RANKING",
            "https://n.news.naver.com/article/001/1?utm=1",
            "Sun, 21 Jun 2026 10:00:00 +0900"
        );
    }
}
