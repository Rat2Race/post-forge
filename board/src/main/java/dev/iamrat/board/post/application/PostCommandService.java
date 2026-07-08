package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostPolicy;
import dev.iamrat.board.post.domain.event.PostCreatedEvent;
import dev.iamrat.board.post.domain.event.PostDeletedEvent;
import dev.iamrat.board.post.domain.event.PostDomainEvent;
import dev.iamrat.board.post.presentation.dto.PostSummaryResponse;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.board.post.PostBoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.event.DomainEventRecorder;
import java.util.List;
import java.util.Locale;
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
    private final AccountProfileReader accountProfileReader;
    private final List<DomainEventRecorder> domainEventRecorders;
    private final PostPolicy postPolicy = new PostPolicy();

    @Transactional
    public PostSummaryResponse savePost(String title, String content, Long accountId, List<Long> fileIds) {
        return savePost(title, content, null, null, accountId, fileIds);
    }

    @Transactional
    public PostSummaryResponse savePost(
        String title,
        String content,
        List<String> tags,
        PostBoardCategory boardCategory,
        Long accountId,
        List<Long> fileIds
    ) {
        postPolicy.validateAuthor(accountId);
        String nickname = accountProfileReader.getProfile(accountId).nickname();

        Post newPost = Post.create(
            title,
            content,
            null,
            tags,
            PostCategory.GENERAL,
            resolveBoardCategory(boardCategory, tags),
            accountId,
            nickname
        );

        postStore.save(newPost);
        postFileAppender.appendFiles(newPost, fileIds);
        record(PostCreatedEvent.from(newPost));

        return PostSummaryResponse.from(newPost);
    }

    @Transactional
    public PostSummaryResponse updatePost(Long postId, String title, String content, List<Long> fileIds) {
        return updatePost(postId, title, content, null, null, fileIds);
    }

    @Transactional
    public PostSummaryResponse updatePost(
        Long postId,
        String title,
        String content,
        List<String> tags,
        PostBoardCategory boardCategory,
        List<Long> fileIds
    ) {
        Post post = postReader.getById(postId);

        post.update(title, content, tags, PostCategory.GENERAL, resolveBoardCategory(boardCategory, tags));
        postFileAppender.replaceFiles(post, fileIds);

        return PostSummaryResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postReader.getById(postId);

        postFileAppender.detachFiles(post);

        postStore.delete(post);
        record(PostDeletedEvent.from(post));
    }

    public boolean isOwner(Long postId, Long accountId) {
        return postPolicy.isOwner(postReader.getById(postId), accountId);
    }

    private void record(PostDomainEvent event) {
        for (DomainEventRecorder recorder : domainEventRecorders) {
            recorder.record(
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event
            );
        }
    }

    private PostBoardCategory resolveBoardCategory(PostBoardCategory requestedBoardCategory, List<String> tags) {
        if (requestedBoardCategory != null) {
            return requestedBoardCategory;
        }
        if (tags == null) {
            return PostBoardCategory.GENERAL;
        }
        return tags.stream()
            .map(this::normalizeTag)
            .map(this::inferBoardCategory)
            .filter(category -> category != null)
            .findFirst()
            .orElse(PostBoardCategory.GENERAL);
    }

    private String normalizeTag(String tag) {
        return tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
    }

    private PostBoardCategory inferBoardCategory(String tag) {
        return switch (tag) {
            case "digital", "디지털", "it", "tech", "테크" -> PostBoardCategory.DIGITAL;
            case "appliance", "가전", "주방가전" -> PostBoardCategory.APPLIANCE;
            case "living", "life", "리빙", "생활" -> PostBoardCategory.LIVING;
            case "health", "건강", "헬스" -> PostBoardCategory.HEALTH;
            case "beauty", "뷰티", "화장품" -> PostBoardCategory.BEAUTY;
            case "sports", "sport", "스포츠", "운동" -> PostBoardCategory.SPORTS;
            default -> null;
        };
    }
}
