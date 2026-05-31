package dev.iamrat.board.autopost.application;

import dev.iamrat.board.post.application.PostStore;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostProductLink;
import dev.iamrat.board.autopost.domain.AutoPostDraft;
import dev.iamrat.board.post.infrastructure.persistence.PostProductLinkRepository;
import dev.iamrat.board.autopost.infrastructure.persistence.AutoPostDraftRepository;
import dev.iamrat.core.board.post.AutoPriceDropPostWriteCommand;
import dev.iamrat.core.board.post.AutoPriceDropPostWriteResult;
import dev.iamrat.core.board.post.AutoPriceDropPostWriter;
import dev.iamrat.core.board.post.PostCategory;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AutoPriceDropPostWriterAdapter implements AutoPriceDropPostWriter {

    private static final long SYSTEM_ACCOUNT_ID = 0L;
    private static final String SYSTEM_NICKNAME = "PostForge Price Bot";

    private final PostStore postStore;
    private final PostProductLinkRepository postProductLinkRepository;
    private final AutoPostDraftRepository autoPostDraftRepository;

    @Override
    @Transactional
    public AutoPriceDropPostWriteResult writeAutoPriceDropPost(AutoPriceDropPostWriteCommand command) {
        LocalDate postDate = command.postDate() == null ? LocalDate.now() : command.postDate();
        if (autoPostDraftRepository.existsByEventId(command.eventId())) {
            return AutoPriceDropPostWriteResult.skipped("duplicate_auto_post_event");
        }
        if (postProductLinkRepository.existsByProductIdAndPostDate(command.productId(), postDate)) {
            return AutoPriceDropPostWriteResult.skipped("duplicate_product_auto_post");
        }

        AutoPostDraft draft = autoPostDraftRepository.save(AutoPostDraft.create(command));
        if (!command.publishNow()) {
            return AutoPriceDropPostWriteResult.drafted(draft.getId());
        }

        try {
            Post post = Post.create(
                command.title(),
                command.content(),
                command.summary(),
                command.tags(),
                PostCategory.AI_ANALYSIS,
                SYSTEM_ACCOUNT_ID,
                SYSTEM_NICKNAME
            );
            Post savedPost = postStore.save(post);
            postProductLinkRepository.save(PostProductLink.of(savedPost, command.productId(), postDate));
            draft.markPublished(savedPost.getId());
            return AutoPriceDropPostWriteResult.published(draft.getId(), savedPost.getId());
        } catch (DataIntegrityViolationException e) {
            return AutoPriceDropPostWriteResult.skipped("duplicate_product_auto_post");
        }
    }
}
