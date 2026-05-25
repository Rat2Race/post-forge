package dev.iamrat.board.view.infrastructure.redis;

import dev.iamrat.board.view.application.ViewCountStore;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import static dev.iamrat.board.view.infrastructure.redis.ViewCountRedisKeys.VIEW_DIRTY_KEY;
import static dev.iamrat.board.view.infrastructure.redis.ViewCountRedisKeys.processingDirtyKey;
import static dev.iamrat.board.view.infrastructure.redis.ViewCountRedisKeys.viewCountKey;
import static dev.iamrat.board.view.infrastructure.redis.ViewCountRedisKeys.viewGuardKey;

@Repository
@RequiredArgsConstructor
public class ViewCountRedisRepository implements ViewCountStore {

    private static final long VIEW_GUARD_TTL_HOURS = 24;
    private static final long CACHE_TTL_SECONDS = 86_400;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean markViewedIfAbsent(Long postId, Long accountId) {
        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(viewGuardKey(postId, accountId), "Viewed", VIEW_GUARD_TTL_HOURS, TimeUnit.HOURS);
        return Boolean.TRUE.equals(isNew);
    }

    @Override
    public Optional<Long> findViewCount(Long postId) {
        return parseCount(redisTemplate.opsForValue().get(viewCountKey(postId)));
    }

    @Override
    public Optional<Long> findViewCountAndRefreshTtl(Long postId) {
        Optional<Long> viewCount = findViewCount(postId);
        viewCount.ifPresent(ignored -> refreshViewCountTtl(postId));
        return viewCount;
    }

    @Override
    public Map<Long, Long> findViewCounts(List<Long> postIds) {
        List<String> keys = postIds.stream()
            .map(ViewCountRedisKeys::viewCountKey)
            .toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            Long postId = postIds.get(i);
            String value = values != null ? values.get(i) : null;
            parseCount(value).ifPresent(count -> result.put(postId, count));
        }
        return result;
    }

    @Override
    public void incrementViewCount(Long postId) {
        redisTemplate.opsForValue().increment(viewCountKey(postId));
        refreshViewCountTtl(postId);
    }

    @Override
    public void cacheViewCountIfAbsent(Long postId, Long views) {
        redisTemplate.opsForValue().setIfAbsent(
            viewCountKey(postId),
            String.valueOf(views),
            CACHE_TTL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    @Override
    public void cacheViewCountsIfAbsent(Map<Long, Long> viewCounts) {
        if (viewCounts.isEmpty()) {
            return;
        }

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (var entry : viewCounts.entrySet()) {
                byte[] key = viewCountKey(entry.getKey()).getBytes(StandardCharsets.UTF_8);
                byte[] value = String.valueOf(entry.getValue()).getBytes(StandardCharsets.UTF_8);
                connection.stringCommands().setNX(key, value);
                connection.keyCommands().expire(key, CACHE_TTL_SECONDS);
            }
            return null;
        });
    }

    @Override
    public void markDirty(Long postId) {
        redisTemplate.opsForSet().add(VIEW_DIRTY_KEY, String.valueOf(postId));
    }

    @Override
    public void deleteViewCount(Long postId) {
        redisTemplate.delete(viewCountKey(postId));
    }

    @Override
    public Optional<String> claimDirtyIdsForProcessing() {
        String processingKey = processingDirtyKey();
        Set<String> processingDirtyIds = redisTemplate.opsForSet().members(processingKey);
        if (processingDirtyIds != null && !processingDirtyIds.isEmpty()) {
            return Optional.of(processingKey);
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(processingKey))) {
            redisTemplate.delete(processingKey);
        }

        try {
            redisTemplate.rename(VIEW_DIRTY_KEY, processingKey);
            return Optional.of(processingKey);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Set<String> findDirtyIds(String dirtyKey) {
        Set<String> dirtyIds = redisTemplate.opsForSet().members(dirtyKey);
        return dirtyIds == null ? Collections.emptySet() : dirtyIds;
    }

    @Override
    public void deleteDirtyIds(String dirtyKey) {
        redisTemplate.delete(dirtyKey);
    }

    @Override
    public void removeProcessedDirtyIds(String processingKey, Set<String> processedDirtyIds) {
        if (processedDirtyIds.isEmpty()) {
            return;
        }

        redisTemplate.opsForSet().remove(processingKey, processedDirtyIds.toArray());
        Long remaining = redisTemplate.opsForSet().size(processingKey);
        if (remaining == null || remaining == 0L) {
            redisTemplate.delete(processingKey);
        }
    }

    private void refreshViewCountTtl(Long postId) {
        redisTemplate.expire(viewCountKey(postId), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private Optional<Long> parseCount(String value) {
        return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
    }

}
