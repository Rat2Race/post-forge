package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.LaunchNewsPostDraftCommand;
import dev.iamrat.core.board.post.LaunchNewsPostDraftGenerator;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceLinkCommand;
import dev.iamrat.core.board.post.PostReferenceLinkReader;
import dev.iamrat.core.board.post.PostReferenceLinkWriter;
import dev.iamrat.core.board.post.PostReferenceProvider;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PublishLaunchNewsUseCase {

    private static final List<String> DEFAULT_TOPICS = List.of("신제품", "출시", "공개", "사전예약");
    private static final Long SYSTEM_ACCOUNT_ID = 0L;
    private static final String SYSTEM_NICKNAME = "PostForge News Bot";
    private final NewsSourceClient newsSourceClient;
    private final LaunchNewsEligibilityPolicy launchNewsEligibilityPolicy;
    private final LaunchNewsPostDraftGenerator draftGenerator;
    private final PostWriter postWriter;
    private final PostReferenceLinkReader referenceLinkReader;
    private final PostReferenceLinkWriter referenceLinkWriter;
    private final Clock clock;

    public PublishLaunchNewsUseCase(
        NewsSourceClient newsSourceClient,
        LaunchNewsEligibilityPolicy launchNewsEligibilityPolicy,
        LaunchNewsPostDraftGenerator draftGenerator,
        PostWriter postWriter,
        PostReferenceLinkReader referenceLinkReader,
        PostReferenceLinkWriter referenceLinkWriter,
        Clock clock
    ) {
        this.newsSourceClient = newsSourceClient;
        this.launchNewsEligibilityPolicy = launchNewsEligibilityPolicy;
        this.draftGenerator = draftGenerator;
        this.postWriter = postWriter;
        this.referenceLinkReader = referenceLinkReader;
        this.referenceLinkWriter = referenceLinkWriter;
        this.clock = clock;
    }

    @Transactional
    public LaunchNewsPublishResult publish(LaunchNewsPublishCommand command) {
        List<LaunchNewsCandidate> candidates = collectCandidates(command);
        List<Long> createdPostIds = new ArrayList<>();
        List<LaunchNewsSkip> skips = new ArrayList<>();
        Set<String> seenCanonicalUrls = new HashSet<>();
        Map<DailyCapKey, Long> dailyCounts = new HashMap<>();

        for (LaunchNewsCandidate candidate : candidates) {
            if (!seenCanonicalUrls.add(candidate.canonicalUrl())) {
                skips.add(skip(candidate, LaunchNewsSkipReason.DUPLICATE_ARTICLE));
                continue;
            }
            if (referenceLinkReader.existsByCanonicalUrl(candidate.canonicalUrl())) {
                skips.add(skip(candidate, LaunchNewsSkipReason.DUPLICATE_ARTICLE));
                continue;
            }

            var gateDecision = launchNewsEligibilityPolicy.evaluate(candidate);
            if (gateDecision.isPresent()) {
                skips.add(skip(candidate, gateDecision.get()));
                continue;
            }

            if (dailyCapExceeded(candidate, command.dailyCap(), dailyCounts)) {
                skips.add(skip(candidate, LaunchNewsSkipReason.DAILY_CAP_EXCEEDED));
                continue;
            }

            LaunchNewsPostDraft draft = draftGenerator.generate(toDraftCommand(candidate))
                .orElse(null);
            if (draft == null) {
                skips.add(skip(candidate, LaunchNewsSkipReason.AI_GENERATION_FAILED));
                continue;
            }

            Long postId = postWriter.write(new PostWriteCommand(
                draft.title(),
                draft.content(),
                draft.summary(),
                draft.tags(),
                SYSTEM_ACCOUNT_ID,
                SYSTEM_NICKNAME,
                PostCategory.PRODUCT_LAUNCH_NEWS,
                command.publishOrigin()
            ));
            referenceLinkWriter.write(toReferenceCommand(postId, candidate, command.publishOrigin()));
            incrementDailyCount(candidate, dailyCounts);
            createdPostIds.add(postId);
        }

        return new LaunchNewsPublishResult(
            command.keyword(),
            createdPostIds,
            skips
        );
    }

    private List<LaunchNewsCandidate> collectCandidates(LaunchNewsPublishCommand command) {
        List<LaunchNewsCandidate> candidates = new ArrayList<>();
        for (String query : toQueries(command.keyword(), command.topics())) {
            newsSourceClient.search(new NewsSourceQuery(query, command.displayCount(), "date"))
                .items()
                .stream()
                .map(item -> toCandidate(command, item))
                .forEach(candidates::add);
        }
        return List.copyOf(candidates);
    }

    private LaunchNewsCandidate toCandidate(LaunchNewsPublishCommand command, NewsSourceItem item) {
        String originalUrl = firstNonBlank(item.originalLink(), item.link());
        String canonicalUrl = canonicalize(originalUrl);
        return new LaunchNewsCandidate(
            command.keyword(),
            command.productId(),
            item,
            canonicalUrl,
            originalUrl,
            host(canonicalUrl),
            parsePublishedAt(item.publishedAt())
        );
    }

    private List<String> toQueries(String keyword, List<String> topics) {
        List<String> effectiveTopics = topics.isEmpty() ? DEFAULT_TOPICS : topics;
        return effectiveTopics.stream()
            .map(topic -> keyword + " " + topic)
            .distinct()
            .toList();
    }

    private boolean dailyCapExceeded(
        LaunchNewsCandidate candidate,
        int dailyCap,
        Map<DailyCapKey, Long> dailyCounts
    ) {
        DailyCapKey key = dailyCapKey(candidate);
        long count = dailyCounts.computeIfAbsent(key, ignored -> referenceLinkReader.countByKeywordAndProductIdOnDate(
            key.keyword(),
            candidate.productId(),
            key.publishedDate()
        ));
        return count >= dailyCap;
    }

    private void incrementDailyCount(LaunchNewsCandidate candidate, Map<DailyCapKey, Long> dailyCounts) {
        DailyCapKey key = dailyCapKey(candidate);
        dailyCounts.compute(key, (ignored, count) -> count == null ? 1L : count + 1L);
    }

    private DailyCapKey dailyCapKey(LaunchNewsCandidate candidate) {
        LocalDate fallback = LocalDate.now(clock);
        return new DailyCapKey(
            candidate.keyword().toLowerCase(Locale.ROOT),
            candidate.productId(),
            candidate.publishedDate(fallback)
        );
    }

    private LaunchNewsPostDraftCommand toDraftCommand(LaunchNewsCandidate candidate) {
        return new LaunchNewsPostDraftCommand(
            candidate.keyword(),
            candidate.title(),
            candidate.description(),
            candidate.canonicalUrl(),
            candidate.sourceName(),
            candidate.publishedAtText()
        );
    }

    private PostReferenceLinkCommand toReferenceCommand(
        Long postId,
        LaunchNewsCandidate candidate,
        PostPublishOrigin publishOrigin
    ) {
        return new PostReferenceLinkCommand(
            postId,
            candidate.keyword(),
            candidate.productId(),
            PostReferenceProvider.NAVER_NEWS,
            candidate.canonicalUrl(),
            candidate.originalUrl(),
            candidate.sourceName(),
            referencePublishedAt(candidate),
            candidate.title(),
            publishOrigin
        );
    }

    private LocalDateTime referencePublishedAt(LaunchNewsCandidate candidate) {
        return candidate.publishedAt() == null ? LocalDateTime.now(clock) : candidate.publishedAt();
    }

    private LaunchNewsSkip skip(LaunchNewsCandidate candidate, LaunchNewsSkipReason reason) {
        return new LaunchNewsSkip(candidate.canonicalUrl(), reason);
    }

    private LocalDateTime parsePublishedAt(String value) {
        String normalized = firstNonBlank(value, null);
        if (normalized == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(normalized, DateTimeFormatter.RFC_1123_DATE_TIME)
                .withZoneSameInstant(clock.getZone())
                .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String canonicalize(String url) {
        String normalized = url == null ? "" : url.trim();
        try {
            URI uri = URI.create(normalized);
            URI canonical = new URI(
                uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT),
                uri.getAuthority() == null ? null : uri.getAuthority().toLowerCase(Locale.ROOT),
                trimTrailingSlash(uri.getPath()),
                null,
                null
            );
            return canonical.toString();
        } catch (Exception ignored) {
            int queryStart = normalized.indexOf('?');
            return queryStart >= 0 ? normalized.substring(0, queryStart) : normalized;
        }
    }

    private String host(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private String trimTrailingSlash(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private record DailyCapKey(String keyword, Long productId, LocalDate publishedDate) {
    }
}
