package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostPolicy;
import dev.iamrat.board.post.presentation.dto.PostSummaryResponse;
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
    private final ViewCountService viewCountService;
    private final AccountProfileReader accountProfileReader;
    private final PostPolicy postPolicy = new PostPolicy();

    @Transactional
    public PostSummaryResponse savePost(String title, String content, Long accountId) {
        return savePost(title, content, null, null, PostCategory.GENERAL, accountId);
    }

    @Transactional
    public PostSummaryResponse savePost(
        String title,
        String content,
        String summary,
        List<String> tags,
        PostCategory category,
        Long accountId
    ) {
        postPolicy.validateAuthor(accountId);
        String nickname = accountProfileReader.getProfile(accountId).nickname();

        Post newPost = Post.create(title, content, summary, tags, category, accountId, nickname);

        postStore.save(newPost);

        return PostSummaryResponse.from(newPost);
    }

    @Transactional
    public PostSummaryResponse updatePost(Long postId, String title, String content) {
        return updatePost(postId, title, content, null, null, PostCategory.GENERAL);
    }

    @Transactional
    public PostSummaryResponse updatePost(
        Long postId,
        String title,
        String content,
        String summary,
        List<String> tags,
        PostCategory category
    ) {
        Post post = postReader.getById(postId);

        post.update(title, content, summary, tags, category);

        return PostSummaryResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postReader.getById(postId);

        viewCountService.deleteViewCount(postId);

        postStore.delete(post);
    }

    public boolean isOwner(Long postId, Long accountId) {
        return postPolicy.isOwner(postReader.getById(postId), accountId);
    }
}
