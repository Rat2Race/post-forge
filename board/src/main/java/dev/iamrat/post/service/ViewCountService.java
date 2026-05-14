package dev.iamrat.post.service;

import dev.iamrat.board.exception.BoardErrorCode;
import dev.iamrat.global.exception.CommonErrorCode;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.post.entity.Post;
import dev.iamrat.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static dev.iamrat.post.support.ViewCountRedisKeys.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountService {
    private static final long VIEW_GUARD_TTL_HOURS = 24;
    private static final long CACHE_TTL_SECONDS = 86400; // 24시간


    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;

    public void incrementIfNew(Long postId, String visitorId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        if (visitorId == null || visitorId.isBlank()) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        
        String guardKey = VIEW_GUARD_PREFIX + postId + ":" + visitorId;

        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(guardKey, "Viewed", VIEW_GUARD_TTL_HOURS, TimeUnit.HOURS);

        if (Boolean.TRUE.equals(isNew)) {
            String viewKey = VIEW_COUNT_PREFIX + postId;
            if (redisTemplate.opsForValue().get(viewKey) == null) {
                loadFromDb(postId);
            }
            redisTemplate.opsForValue().increment(viewKey);
            redisTemplate.expire(viewKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            redisTemplate.opsForSet().add(VIEW_DIRTY_KEY, String.valueOf(postId));
        }
    }

    public long getViewCount(Long postId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        String key = VIEW_COUNT_PREFIX + postId;
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            return Long.parseLong(cached);
        }

        return loadFromDb(postId);
    }

    public Map<Long, Long> getViewCounts(List<Long> postIds) {
        if (postIds == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> keys = postIds.stream()
                .map(id -> VIEW_COUNT_PREFIX + id)
                .toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Long> result = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();

        for (int i = 0; i < postIds.size(); i++) {
            String value = values != null ? values.get(i) : null;

            if (value != null) {
                result.put(postIds.get(i), Long.parseLong(value));
            } else {
                missedIds.add(postIds.get(i));
            }
        }

        if (!missedIds.isEmpty()) {
            result.putAll(loadFromDb(missedIds));
        }

        return result;
    }

    public void deleteViewCount(Long postId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        redisTemplate.delete(VIEW_COUNT_PREFIX + postId);
    }

    private long loadFromDb(Long postId) {
        Long views = postRepository.findById(postId)
            .map(Post::getViews)
            .orElseThrow(() -> new CustomException(BoardErrorCode.POST_NOT_FOUND));
        
        redisTemplate.opsForValue().setIfAbsent(
            VIEW_COUNT_PREFIX + postId, String.valueOf(views),
            CACHE_TTL_SECONDS, TimeUnit.SECONDS
        );
        
        return views;
    }

    private Map<Long, Long> loadFromDb(List<Long> postIds) {
        Map<Long, Long> result = new HashMap<>();

        postRepository.findAllById(postIds).forEach(post -> {
            long views = post.getViews();
            result.put(post.getId(), views);
        });

        if (!result.isEmpty()) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (var entry : result.entrySet()) {
                    byte[] key = (VIEW_COUNT_PREFIX + entry.getKey()).getBytes();
                    byte[] value = String.valueOf(entry.getValue()).getBytes();
                    connection.stringCommands().setNX(key, value);
                    connection.keyCommands().expire(key, CACHE_TTL_SECONDS);
                }
                return null;
            });
        }

        return result;
    }
}
