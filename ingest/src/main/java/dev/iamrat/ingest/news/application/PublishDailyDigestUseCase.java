package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.DailyDigestDraft;
import dev.iamrat.core.board.post.DailyDigestDraftCommand;
import dev.iamrat.core.board.post.DailyDigestDraftGenerator;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.NewsPostPort;
import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.ingest.news.application.DailyDigestPublishResult.SkipReason;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublishDailyDigestUseCase {

    private final NewsPostPort newsPosts;
    private final DailyDigestDraftGenerator draftGenerator;

    public DailyDigestPublishResult publish(LocalDate newsDate) {
        List<Long> createdPostIds = new ArrayList<>();
        Map<NewsSection, SkipReason> skips = new LinkedHashMap<>();

        for (NewsSection section : NewsSection.values()) {
            List<DailyDigestSourceItem> items = newsPosts.findLaunchNews(section, newsDate);
            if (items.isEmpty()) {
                skips.put(section, SkipReason.NO_SOURCE);
                continue;
            }

            if (newsPosts.digestExists(section, newsDate)) {
                skips.put(section, SkipReason.ALREADY_PUBLISHED);
                continue;
            }

            DailyDigestDraft draft = draftGenerator.generate(new DailyDigestDraftCommand(section, newsDate, items))
                .orElse(null);
            if (draft == null) {
                skips.put(section, SkipReason.AI_GENERATION_FAILED);
                continue;
            }

            createdPostIds.add(newsPosts.publishDailyDigest(section, newsDate, draft));
        }

        return new DailyDigestPublishResult(newsDate, createdPostIds, skips);
    }
}
