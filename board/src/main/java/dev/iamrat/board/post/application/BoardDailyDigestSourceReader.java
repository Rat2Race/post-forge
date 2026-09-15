package dev.iamrat.board.post.application;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.DailyDigestSourceReader;
import dev.iamrat.core.board.post.PostCategory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardDailyDigestSourceReader implements DailyDigestSourceReader {

    private final PostStore postStore;

    @Override
    public List<DailyDigestSourceItem> findLaunchNews(BoardCategory category, LocalDate newsDate) {
        LocalDateTime startOfDay = newsDate.atStartOfDay();
        return postStore.findByCategoryAndBoardCategoryInRange(
                PostCategory.PRODUCT_LAUNCH_NEWS,
                category,
                startOfDay,
                startOfDay.plusDays(1)
            )
            .stream()
            .map(post -> new DailyDigestSourceItem(post.getTitle(), post.getSummary()))
            .toList();
    }

    @Override
    public boolean digestExists(BoardCategory category, String title) {
        return postStore.existsByCategoryAndBoardCategoryAndTitle(PostCategory.DAILY_DIGEST, category, title);
    }
}
