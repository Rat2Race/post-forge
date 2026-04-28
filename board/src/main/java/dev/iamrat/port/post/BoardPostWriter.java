package dev.iamrat.port.post;

import dev.iamrat.board.post.PostCategory;
import dev.iamrat.board.post.PostWriter;
import dev.iamrat.post.entity.Post;
import dev.iamrat.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardPostWriter implements PostWriter {

    private final PostRepository postRepository;

    @Override
    @Transactional
    public Long write(String title, String content, String summary, List<String> tags,
                      String userId, String nickname, PostCategory category) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .summary(summary)
                .tags(tags)
                .category(category)
                .userId(userId)
                .nickname(nickname)
                .build();
        return postRepository.save(post).getId();
    }
}
