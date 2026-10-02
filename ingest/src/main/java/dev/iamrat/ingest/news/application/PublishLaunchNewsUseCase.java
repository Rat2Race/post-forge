package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.LaunchNewsPost;
import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.LaunchNewsPostDraftCommand;
import dev.iamrat.core.board.post.LaunchNewsPostDraftGenerator;
import dev.iamrat.core.board.post.NewsPostPort;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult.Skip;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult.SkipReason;
import dev.iamrat.source.news.application.NewsSourceItem;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublishLaunchNewsUseCase {

    private static final Set<String> TRACKING_PARAM_NAMES = Set.of("utm", "ntype", "sid", "fbclid", "gclid");
    private final IngestProductNewsUseCase ingestProductNewsUseCase;
    private final LaunchNewsEligibilityPolicy launchNewsEligibilityPolicy;
    private final LaunchNewsPostDraftGenerator draftGenerator;
    private final NewsPostPort newsPosts;
    private final Clock clock;


    public LaunchNewsPublishResult publish(LaunchNewsPublishCommand command) {
        List<LaunchNewsCandidate> candidates = collectCandidates(command);
        List<Long> createdPostIds = new ArrayList<>();
        List<Skip> skips = new ArrayList<>();
        Set<String> seenCanonicalUrls = new HashSet<>();
        Map<DailyCapKey, Long> dailyCounts = new HashMap<>();

        for (LaunchNewsCandidate candidate : candidates) {
            if (!seenCanonicalUrls.add(candidate.canonicalUrl())) {
                skips.add(skip(candidate, SkipReason.DUPLICATE_ARTICLE));
                continue;
            }
            if (newsPosts.isPublished(candidate.canonicalUrl())) {
                skips.add(skip(candidate, SkipReason.DUPLICATE_ARTICLE));
                continue;
            }

            var gateDecision = launchNewsEligibilityPolicy.evaluate(candidate);
            if (gateDecision.isPresent()) {
                skips.add(skip(candidate, gateDecision.get()));
                continue;
            }

            if (dailyCapExceeded(candidate, command.dailyCap(), dailyCounts)) {
                skips.add(skip(candidate, SkipReason.DAILY_CAP_EXCEEDED));
                continue;
            }

            LaunchNewsPostDraft draft = draftGenerator.generate(toDraftCommand(candidate))
                .orElse(null);
            if (draft == null) {
                skips.add(skip(candidate, SkipReason.AI_GENERATION_FAILED));
                continue;
            }

            try {
                Long postId = newsPosts.publishLaunchNews(toLaunchNewsPost(draft, candidate, command));
                incrementDailyCount(candidate, dailyCounts);
                createdPostIds.add(postId);
            } catch (DataIntegrityViolationException ignored) {
                skips.add(skip(candidate, SkipReason.DUPLICATE_ARTICLE));
            }
        }

        return new LaunchNewsPublishResult(
            command.keyword(),
            createdPostIds,
            skips
        );
    }

    private List<LaunchNewsCandidate> collectCandidates(LaunchNewsPublishCommand command) {
        return ingestProductNewsUseCase.collectAndIngest(command.keyword(), command.displayCount(), command.topics())
            .items()
            .stream()
            .map(item -> toCandidate(command, item))
            .toList();
    }

    private LaunchNewsCandidate toCandidate(LaunchNewsPublishCommand command, NewsSourceItem item) {
        String originalUrl = firstNonBlank(item.originalLink(), item.link());
        String canonicalUrl = canonicalize(originalUrl);
        return new LaunchNewsCandidate(
            command.keyword(),
            item,
            canonicalUrl,
            originalUrl,
            host(canonicalUrl),
            parsePublishedAt(item.publishedAt()),
            command.category()
        );
    }

    private boolean dailyCapExceeded(
        LaunchNewsCandidate candidate,
        int dailyCap,
        Map<DailyCapKey, Long> dailyCounts
    ) {
        DailyCapKey key = dailyCapKey(candidate);
        long count = dailyCounts.computeIfAbsent(key, ignored -> newsPosts.countPublished(
            key.keyword(),
            key.publishedDate()
        ));
        return count >= dailyCap;
    }

    private void incrementDailyCount(LaunchNewsCandidate candidate, Map<DailyCapKey, Long> dailyCounts) {
        DailyCapKey key = dailyCapKey(candidate);
        dailyCounts.compute(key, (ignored, count) -> count == null ? 1L : count + 1L);
    }

    private DailyCapKey dailyCapKey(LaunchNewsCandidate candidate) {
        return new DailyCapKey(
            candidate.keyword().toLowerCase(Locale.ROOT),
            candidate.publishedAt().toLocalDate()
        );
    }

    private LaunchNewsPost toLaunchNewsPost(
        LaunchNewsPostDraft draft,
        LaunchNewsCandidate candidate,
        LaunchNewsPublishCommand command
    ) {
        return new LaunchNewsPost(
            draft,
            candidate.category(),
            command.publishOrigin(),
            candidate.keyword(),
            candidate.title(),
            candidate.canonicalUrl(),
            candidate.originalUrl(),
            candidate.sourceName(),
            candidate.publishedAt()
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

    private Skip skip(LaunchNewsCandidate candidate, SkipReason reason) {
        return new Skip(candidate.canonicalUrl(), reason);
    }

    // 발행 시각을 읽을 수 없으면 처리 시각으로 대신한다. 일일 상한 날짜와 저장 시각이 같은 값을 쓴다.
    private LocalDateTime parsePublishedAt(String value) {
        String normalized = firstNonBlank(value, null);
        if (normalized == null) {
            return LocalDateTime.now(clock);
        }
        try {
            return ZonedDateTime.parse(normalized, DateTimeFormatter.RFC_1123_DATE_TIME)
                .withZoneSameInstant(clock.getZone())
                .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.now(clock);
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
            String query = stripTrackingParams(uri.getRawQuery());
            return query == null ? canonical.toString() : canonical + "?" + query;
        } catch (Exception ignored) {
            int queryStart = normalized.indexOf('?');
            if (queryStart < 0) {
                return normalized;
            }
            String path = normalized.substring(0, queryStart);
            String query = stripTrackingParams(normalized.substring(queryStart + 1));
            return query == null ? path : path + "?" + query;
        }
    }

    private String stripTrackingParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String kept = Arrays.stream(rawQuery.split("&"))
            .filter(parameter -> !isTrackingParam(parameter))
            .sorted()
            .collect(Collectors.joining("&"));
        return kept.isBlank() ? null : kept;
    }

    private boolean isTrackingParam(String parameter) {
        String name = parameter.split("=", 2)[0].toLowerCase(Locale.ROOT);
        return name.startsWith("utm_") || TRACKING_PARAM_NAMES.contains(name);
    }

    private String host(String url) {
        int queryStart = url.indexOf('?');
        String withoutQuery = queryStart >= 0 ? url.substring(0, queryStart) : url;
        try {
            String host = URI.create(withoutQuery).getHost();
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

    private record DailyCapKey(String keyword, LocalDate publishedDate) {
    }
}
