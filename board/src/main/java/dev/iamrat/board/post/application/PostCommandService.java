package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostPolicy;
import dev.iamrat.board.post.presentation.PostSummaryResponse;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.board.post.PostCategory;
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
    private final PostPolicy postPolicy = new PostPolicy();

    @Transactional
    public PostSummaryResponse savePost(
        String title,
        String content,
        List<String> tags,
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
            accountId,
            nickname
        );

        postStore.save(newPost);
        postFileAppender.appendFiles(newPost, fileIds);

        return PostSummaryResponse.from(newPost);
    }

    @Transactional
    public PostSummaryResponse updatePost(
        Long postId,
        String title,
        String content,
        List<String> tags,
        List<Long> fileIds
    ) {
        Post post = postReader.getById(postId);

        post.update(title, content, tags);
        postFileAppender.replaceFiles(post, fileIds);

        return PostSummaryResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postReader.getById(postId);

        postFileAppender.detachFiles(post);
        viewCountService.deleteViewCount(postId);

        postStore.delete(post);
    }

    public boolean isOwner(Long postId, Long accountId) {
        return postPolicy.isOwner(postReader.getById(postId), accountId);
    }

}
