package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Locale;

@Slf4j
@Component
@ConditionalOnProperty(
    name = {"ingest.news.launch.scheduler.enabled", "source.google-news.enabled"},
    havingValue = "true"
)
public class LaunchNewsPublishScheduler {

    private final PublishLaunchNewsUseCase publishLaunchNewsUseCase;
    private final List<String> sections;
    private final int displayCount;
    private final int dailyCap;

    public LaunchNewsPublishScheduler(PublishLaunchNewsUseCase publishLaunchNewsUseCase,
                                     @Value("${ingest.news.launch.sections:TECHNOLOGY,BUSINESS}") List<String> sections,
                                     @Value("${ingest.news.launch.display-count:5}") int displayCount,
                                     @Value("${ingest.news.launch.daily-cap:3}") int dailyCap) {
        this.publishLaunchNewsUseCase = publishLaunchNewsUseCase;
        // 기본값 "A,B"는 ConversionService가 없는 컨텍스트에서 한 원소로 들어오므로 여기서도 쉼표를 나눈다.
        this.sections = sections.stream().flatMap(value -> Arrays.stream(value.split(",")))
            .map(String::trim).filter(section -> !section.isEmpty())
            .map(section -> section.toUpperCase(Locale.ROOT)).distinct().toList();
        this.sections.forEach(BoardCategory::valueOf); // 섹션 이름은 BoardCategory와 같아야 한다
        if (this.sections.isEmpty() || displayCount < 1 || displayCount > 100 || dailyCap < 1 || dailyCap > 20) {
            throw new IllegalArgumentException("수집 섹션과 1~100 범위의 수집 건수, 1~20 범위의 일일 상한이 필요합니다");
        }
        this.displayCount = displayCount;
        this.dailyCap = dailyCap;
    }

    @Scheduled(cron = "${ingest.news.launch.cron:0 */10 * * * *}", zone = "Asia/Seoul")
    public void publishSectionNews() {
        for (String section : sections) {
            try {
                LaunchNewsPublishResult result = publishLaunchNewsUseCase.publish(new LaunchNewsPublishCommand(
                    section,
                    displayCount,
                    dailyCap,
                    List.of(),
                    BoardCategory.valueOf(section),
                    PostPublishOrigin.SYSTEM_BATCH
                ));
                log.info("섹션 뉴스 게시 완료. section={}, published={}, skipped={}",
                    section, result.publishedCount(), result.skippedCount());
            } catch (RuntimeException exception) {
                log.warn("섹션 뉴스 게시 실패. section={}", section, exception);
            }
        }
    }
}
