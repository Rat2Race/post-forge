package dev.iamrat.board.post.service;

import dev.iamrat.board.post.repository.PostRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static dev.iamrat.board.post.support.ViewCountRedisKeys.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {
    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void syncViewCountsToDb() {
        String processingKey = claimProcessingKey();
        if (processingKey == null) {
            return;
        }

        SetOperations<String, String> setOperations = redisTemplate.opsForSet();
        Set<String> processingDirtyIds = setOperations.members(processingKey);
        if (processingDirtyIds == null || processingDirtyIds.isEmpty()) {
            redisTemplate.delete(processingKey);
            return;
        }

        Set<String> processedDirtyIds = new LinkedHashSet<>();
        int synced = 0;
        for (String dirtyId : processingDirtyIds) {
            try {
                Long postId = Long.parseLong(dirtyId);
                String countStr = redisTemplate.opsForValue()
                    .get(VIEW_COUNT_PREFIX + postId);
                if (countStr != null) {
                    long views = Long.parseLong(countStr);
                    postRepository.updateViews(postId, views);
                    synced++;
                }
                processedDirtyIds.add(dirtyId);
            } catch (NumberFormatException e) {
                log.warn("조회수 dirty ID 파싱 실패: {}", dirtyId);
                processedDirtyIds.add(dirtyId);
            } catch (Exception e) {
                log.warn("조회수 동기화 재시도 예정: dirtyId={}", dirtyId, e);
            }
        }

        removeProcessedDirtyIds(processingKey, processedDirtyIds);

        if (synced > 0) {
            log.info("조회수 DB 동기화 완료: {}건", synced);
        }
    }

    private String claimProcessingKey() {
        String processingKey = VIEW_DIRTY_KEY + ":processing";
        Set<String> processingDirtyIds = redisTemplate.opsForSet().members(processingKey);
        if (processingDirtyIds != null && !processingDirtyIds.isEmpty()) {
            return processingKey;
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(processingKey))) {
            redisTemplate.delete(processingKey);
        }

        try {
            redisTemplate.rename(VIEW_DIRTY_KEY, processingKey);
            return processingKey;
        } catch (Exception e) {
            return null;
        }
    }

    private void removeProcessedDirtyIds(String processingKey, Set<String> processedDirtyIds) {
        if (processedDirtyIds.isEmpty()) {
            return;
        }

        redisTemplate.opsForSet().remove(processingKey, processedDirtyIds.toArray());
        Long remaining = redisTemplate.opsForSet().size(processingKey);
        if (remaining == null || remaining == 0L) {
            redisTemplate.delete(processingKey);
        }
    }
}
