package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostPolicy;
import dev.iamrat.board.post.domain.event.PostCreatedEvent;
import dev.iamrat.board.post.domain.event.PostDeletedEvent;
import dev.iamrat.board.post.domain.event.PostDomainEvent;
import dev.iamrat.board.post.dto.PostSummaryResponse;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.event.DomainEventRecorder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommandService {

    private final PostStore postStore;
    private final PostReader postReader;
    private final PostFileAppender postFileAppender;
    private final ViewCountService viewCountService;
    private final AccountProfileReader accountProfileReader;
    private final DomainEventRecorder domainEventRecorder;
    private final PostPolicy postPolicy = new PostPolicy();

    @Transactional
    public PostSummaryResponse savePost(String title, String content, Long accountId, List<Long> fileIds) {
        postPolicy.validateAuthor(accountId);
        String nickname = accountProfileReader.getProfile(accountId).nickname();

        Post newPost = Post.general(title, content, accountId, nickname);

        postStore.save(newPost);
        postFileAppender.appendFiles(newPost, fileIds);
        record(PostCreatedEvent.from(newPost));

        return PostSummaryResponse.from(newPost);
    }

    @Transactional
    public PostSummaryResponse updatePost(Long postId, String title, String content, List<Long> fileIds) {
        Post post = postReader.getById(postId);

        post.update(title, content);
        postFileAppender.replaceFiles(post, fileIds);

        return PostSummaryResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postReader.getById(postId);

        postFileAppender.detachFiles(post);
        viewCountService.deleteViewCount(postId);

        postStore.delete(post);
        record(PostDeletedEvent.from(post));
    }

    public boolean isOwner(Long postId, Long accountId) {
        return postPolicy.isOwner(postReader.getById(postId), accountId);
    }

    private void record(PostDomainEvent event) {
        domainEventRecorder.record(
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event
        );
    }
}
