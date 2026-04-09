package dev.iamrat.post.service;

import dev.iamrat.post.repository.PostRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        Set<Long> dirtyIds = popDirtyIds();
        if (dirtyIds.isEmpty()) {
            return;
        }

        int synced = 0;
        for (Long postId : dirtyIds) {
            try {
                String countStr = redisTemplate.opsForValue()
                    .get(VIEW_COUNT_PREFIX + postId);
                if (countStr != null) {
                    long views = Long.parseLong(countStr);
                    postRepository.updateViews(postId, views);
                    synced++;
                }
            } catch (NumberFormatException e) {
                log.warn("조회수 동기화 파싱 실패: postId={}", postId);
            }
        }

        if (synced > 0) {
            log.info("조회수 DB 동기화 완료: {}건", synced);
        }
    }

    private Set<Long> popDirtyIds() {
        String processingKey = VIEW_DIRTY_KEY + ":processing";
        try {
            redisTemplate.rename(VIEW_DIRTY_KEY, processingKey);
        } catch (Exception e) {
            return Collections.emptySet();
        }

        Set<String> members = redisTemplate.opsForSet().members(processingKey);
        redisTemplate.delete(processingKey);

        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> ids = new HashSet<>();
        for (String member : members) {
            try {
                ids.add(Long.parseLong(member));
            } catch (NumberFormatException e) {
                log.warn("조회수 dirty ID 파싱 실패: {}", member);
            }
        }
        return ids;
    }
}
