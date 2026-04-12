package dev.iamrat.post.service;

import dev.iamrat.post.repository.PostRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static dev.iamrat.post.support.ViewCountRedisKeys.*;


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
        Set<String> processingMembers = setOperations.members(processingKey);
        if (processingMembers == null || processingMembers.isEmpty()) {
            redisTemplate.delete(processingKey);
            return;
        }

        Set<String> processedMembers = new LinkedHashSet<>();
        int synced = 0;
        for (String member : processingMembers) {
            try {
                Long postId = Long.parseLong(member);
                String countStr = redisTemplate.opsForValue()
                    .get(VIEW_COUNT_PREFIX + postId);
                if (countStr != null) {
                    long views = Long.parseLong(countStr);
                    postRepository.updateViews(postId, views);
                    synced++;
                }
                processedMembers.add(member);
            } catch (NumberFormatException e) {
                log.warn("조회수 dirty ID 파싱 실패: {}", member);
                processedMembers.add(member);
            } catch (Exception e) {
                log.warn("조회수 동기화 재시도 예정: dirtyId={}", member, e);
            }
        }

        removeProcessedMembers(processingKey, processedMembers);

        if (synced > 0) {
            log.info("조회수 DB 동기화 완료: {}건", synced);
        }
    }

    private String claimProcessingKey() {
        String processingKey = VIEW_DIRTY_KEY + ":processing";
        Set<String> processingMembers = redisTemplate.opsForSet().members(processingKey);
        if (processingMembers != null && !processingMembers.isEmpty()) {
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

    private void removeProcessedMembers(String processingKey, Set<String> processedMembers) {
        if (processedMembers.isEmpty()) {
            return;
        }

        redisTemplate.opsForSet().remove(processingKey, processedMembers.toArray());
        Long remaining = redisTemplate.opsForSet().size(processingKey);
        if (remaining == null || remaining == 0L) {
            redisTemplate.delete(processingKey);
        }
    }
}
