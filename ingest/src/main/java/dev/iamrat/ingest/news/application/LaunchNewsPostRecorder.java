package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceLinkCommand;
import dev.iamrat.core.board.post.PostReferenceLinkWriter;
import dev.iamrat.core.board.post.PostReferenceProvider;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LaunchNewsPostRecorder {

    private static final Long SYSTEM_ACCOUNT_ID = 0L;
    private static final String SYSTEM_NICKNAME = "PostForge News Bot";
    private final PostWriter postWriter;
    private final PostReferenceLinkWriter referenceLinkWriter;
    private final Clock clock;

    public LaunchNewsPostRecorder(
        PostWriter postWriter,
        PostReferenceLinkWriter referenceLinkWriter,
        Clock clock
    ) {
        this.postWriter = postWriter;
        this.referenceLinkWriter = referenceLinkWriter;
        this.clock = clock;
    }

    @Transactional
    public Long record(LaunchNewsPostDraft draft, LaunchNewsCandidate candidate, PostPublishOrigin publishOrigin) {
        Long postId = postWriter.write(new PostWriteCommand(
            draft.title(),
            draft.content(),
            draft.summary(),
            draft.tags(),
            SYSTEM_ACCOUNT_ID,
            SYSTEM_NICKNAME,
            PostCategory.PRODUCT_LAUNCH_NEWS,
            candidate.category(),
            publishOrigin
        ));
        referenceLinkWriter.write(toReferenceCommand(postId, candidate, publishOrigin));
        return postId;
    }

    private PostReferenceLinkCommand toReferenceCommand(
        Long postId,
        LaunchNewsCandidate candidate,
        PostPublishOrigin publishOrigin
    ) {
        return new PostReferenceLinkCommand(
            postId,
            candidate.keyword(),
            PostReferenceProvider.NAVER_NEWS,
            candidate.canonicalUrl(),
            candidate.originalUrl(),
            candidate.sourceName(),
            referencePublishedAt(candidate),
            candidate.title(),
            publishOrigin
        );
    }

    private LocalDateTime referencePublishedAt(LaunchNewsCandidate candidate) {
        return candidate.publishedAt() == null ? LocalDateTime.now(clock) : candidate.publishedAt();
    }
}
