package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {
    private final ViewCountStore viewCountStore;
    private final PostViewCountService postViewCountService;

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void syncViewCountsToDb() {
        Optional<String> claimedProcessingKey = viewCountStore.claimDirtyIdsForProcessing();
        if (claimedProcessingKey.isEmpty()) {
            return;
        }

        String processingKey = claimedProcessingKey.get();
        Set<String> processingDirtyIds = viewCountStore.findDirtyIds(processingKey);
        if (processingDirtyIds.isEmpty()) {
            viewCountStore.deleteDirtyIds(processingKey);
            return;
        }

        Set<String> processedDirtyIds = new LinkedHashSet<>();
        int synced = 0;
        for (String dirtyId : processingDirtyIds) {
            try {
                Long postId = Long.parseLong(dirtyId);
                Optional<Long> viewCount = viewCountStore.findViewCount(postId);
                if (viewCount.isPresent()) {
                    postViewCountService.updateViewCount(postId, viewCount.get());
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

        viewCountStore.removeProcessedDirtyIds(processingKey, processedDirtyIds);

        if (synced > 0) {
            log.info("조회수 DB 동기화 완료: {}건", synced);
        }
    }
}
