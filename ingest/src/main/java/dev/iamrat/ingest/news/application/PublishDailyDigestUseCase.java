package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.DailyDigestDraft;
import dev.iamrat.core.board.post.DailyDigestDraftCommand;
import dev.iamrat.core.board.post.DailyDigestDraftGenerator;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.DailyDigestSourceReader;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
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

    private static final Long SYSTEM_ACCOUNT_ID = 0L;
    private static final String SYSTEM_NICKNAME = "PostForge News Bot";
    private static final int SUMMARY_MAX_LENGTH = 500;

    private final DailyDigestSourceReader sourceReader;
    private final DailyDigestDraftGenerator draftGenerator;
    private final PostWriter postWriter;

    public DailyDigestPublishResult publish(LocalDate newsDate) {
        List<Long> createdPostIds = new ArrayList<>();
        Map<BoardCategory, DailyDigestSkipReason> skips = new LinkedHashMap<>();

        for (BoardCategory category : BoardCategory.values()) {
            List<DailyDigestSourceItem> items = sourceReader.findLaunchNews(category, newsDate);
            if (items.isEmpty()) {
                skips.put(category, DailyDigestSkipReason.NO_SOURCE);
                continue;
            }

            String title = title(category, newsDate);
            if (sourceReader.digestExists(category, title)) {
                skips.put(category, DailyDigestSkipReason.ALREADY_PUBLISHED);
                continue;
            }

            DailyDigestDraft draft = draftGenerator.generate(new DailyDigestDraftCommand(category, newsDate, items))
                .orElse(null);
            if (draft == null) {
                skips.put(category, DailyDigestSkipReason.AI_GENERATION_FAILED);
                continue;
            }

            createdPostIds.add(postWriter.write(new PostWriteCommand(
                title,
                draft.content(),
                abbreviate(draft.content().replaceAll("\\s+", " "), SUMMARY_MAX_LENGTH),
                draft.tags(),
                SYSTEM_ACCOUNT_ID,
                SYSTEM_NICKNAME,
                PostCategory.DAILY_DIGEST,
                category,
                PostPublishOrigin.SYSTEM_BATCH
            )));
        }

        return new DailyDigestPublishResult(newsDate, createdPostIds, skips);
    }

    private String title(BoardCategory category, LocalDate newsDate) {
        return "[" + displayName(category) + "] 데일리 브리핑 - " + newsDate;
    }

    private String displayName(BoardCategory category) {
        return switch (category) {
            case GENERAL -> "일반";
            case DIGITAL -> "디지털";
            case APPLIANCE -> "가전";
            case LIVING -> "리빙";
            case HEALTH -> "헬스";
            case BEAUTY -> "뷰티";
            case SPORTS -> "스포츠";
        };
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        int end = Character.isHighSurrogate(value.charAt(maxLength - 1)) ? maxLength - 1 : maxLength;
        return value.substring(0, end);
    }
}
