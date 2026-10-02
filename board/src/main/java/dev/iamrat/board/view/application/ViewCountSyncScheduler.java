package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {
    private final ViewCountStore viewCountStore;
    private final PostViewCountService postViewCountService;

    // 트랜잭션을 걸지 않는다. 글마다 updateViewCount가 따로 커밋한 뒤에 dirty 집합에서 지운다.
    // 하나로 묶으면 한 글의 실패가 전체를 롤백시키는데, 지우기(Redis)는 이미 끝나 그 글들의 조회수가 DB에 영영 반영되지 않는다.
    @Scheduled(fixedRate = 300_000)
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
