package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BoardPostWriter implements PostWriter {

    private final PostStore postStore;

    @Override
    @Transactional
    public Long write(PostWriteCommand command) {
        Post post = Post.create(
            command.title(),
            command.content(),
            command.summary(),
            command.tags(),
            command.category(),
            command.accountId(),
            command.nickname()
        );
        return postStore.save(post).getId();
    }
}
