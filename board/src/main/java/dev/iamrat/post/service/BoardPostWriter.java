package dev.iamrat.post.service;

import dev.iamrat.post.PostWriter;
import dev.iamrat.post.entity.Post;
import dev.iamrat.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BoardPostWriter implements PostWriter {

    private final PostRepository postRepository;

    @Override
    @Transactional
    public Long write(String title, String content, String userId) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .userId(userId)
                .build();
        return postRepository.save(post).getId();
    }
}
