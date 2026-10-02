package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.board.post.domain.PostType;
import dev.iamrat.core.board.post.DailyDigestDraft;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.LaunchNewsPost;
import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.NewsPostPort;
import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardNewsPostPort implements NewsPostPort {

    private static final Long BOT_ACCOUNT_ID = 0L;
    private static final String BOT_NICKNAME = "PostForge News Bot";
    private static final int DIGEST_SUMMARY_MAX_LENGTH = 500;

    private final PostStore postStore;
    private final PostReferenceLinkStore postReferenceLinkStore;

    @Override
    public boolean isPublished(String canonicalUrl) {
        return postReferenceLinkStore.existsByCanonicalUrl(canonicalUrl);
    }

    @Override
    public long countPublished(String keyword, LocalDate publishedDate) {
        return postReferenceLinkStore.countByKeywordOnDate(keyword, publishedDate);
    }

    @Override
    @Transactional
    public Long publishLaunchNews(LaunchNewsPost news) {
        LaunchNewsPostDraft draft = news.draft();
        Post post = postStore.save(Post.create(
            draft.title(),
            draft.content(),
            draft.summary(),
            draft.tags(),
            PostType.PRODUCT_LAUNCH_NEWS,
            news.section(),
            news.publishOrigin(),
            BOT_ACCOUNT_ID,
            BOT_NICKNAME
        ));
        postReferenceLinkStore.save(PostReferenceLink.of(
            post,
            news.keyword(),
            news.canonicalUrl(),
            news.originalUrl(),
            news.sourceName(),
            news.publishedAt(),
            news.sourceTitle()
        ));
        return post.getId();
    }

    @Override
    public List<DailyDigestSourceItem> findLaunchNews(NewsSection section, LocalDate newsDate) {
        LocalDateTime startOfDay = newsDate.atStartOfDay();
        return postStore.findByCategoryAndBoardCategoryInRange(
                PostType.PRODUCT_LAUNCH_NEWS,
                section,
                startOfDay,
                startOfDay.plusDays(1)
            )
            .stream()
            .map(post -> new DailyDigestSourceItem(post.getTitle(), post.getSummary()))
            .toList();
    }

    @Override
    public boolean digestExists(NewsSection section, LocalDate newsDate) {
        return postStore.existsByCategoryAndBoardCategoryAndTitle(
            PostType.DAILY_DIGEST, section, digestTitle(section, newsDate));
    }

    @Override
    @Transactional
    public Long publishDailyDigest(NewsSection section, LocalDate newsDate, DailyDigestDraft draft) {
        return postStore.save(Post.create(
            digestTitle(section, newsDate),
            draft.content(),
            abbreviate(draft.content().replaceAll("\\s+", " "), DIGEST_SUMMARY_MAX_LENGTH),
            draft.tags(),
            PostType.DAILY_DIGEST,
            section,
            PostPublishOrigin.SYSTEM_BATCH,
            BOT_ACCOUNT_ID,
            BOT_NICKNAME
        )).getId();
    }

    // digestExists의 멱등 판정이 이 제목에 기댄다. 형식을 바꾸면 기존 브리핑을 못 찾아 중복 게시된다.
    private String digestTitle(NewsSection section, LocalDate newsDate) {
        return "[" + displayName(section) + "] 데일리 브리핑 - " + newsDate;
    }

    private String displayName(NewsSection section) {
        return switch (section) {
            case GENERAL -> "일반";
            case NATION -> "대한민국";
            case WORLD -> "세계";
            case BUSINESS -> "비즈니스";
            case TECHNOLOGY -> "과학/기술";
            case ENTERTAINMENT -> "엔터테인먼트";
            case SPORTS -> "스포츠";
            case HEALTH -> "건강";
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
