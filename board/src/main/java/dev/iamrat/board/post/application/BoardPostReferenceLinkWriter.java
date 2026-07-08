package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.core.board.post.PostReferenceLinkCommand;
import dev.iamrat.core.board.post.PostReferenceLinkWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BoardPostReferenceLinkWriter implements PostReferenceLinkWriter {

    private final PostStore postStore;
    private final PostReferenceLinkStore postReferenceLinkStore;

    @Override
    @Transactional
    public Long write(PostReferenceLinkCommand command) {
        PostReferenceLink referenceLink = PostReferenceLink.of(
            postStore.getReferenceById(command.postId()),
            command.keyword(),
            command.productId(),
            command.provider(),
            command.canonicalUrl(),
            command.originalUrl(),
            command.sourceName(),
            command.publishedAt(),
            command.titleSnapshot(),
            command.publishOrigin()
        );
        return postReferenceLinkStore.save(referenceLink).getId();
    }
}
