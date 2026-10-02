package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult.Skip;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult.SkipReason;
import java.util.EnumMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

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
        this.sections.forEach(NewsSection::valueOf); // 섹션 이름은 NewsSection와 같아야 한다
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
                    NewsSection.valueOf(section),
                    PostPublishOrigin.SYSTEM_BATCH
                ));
                log.info("섹션 뉴스 게시 완료. section={}, published={}, skips={}",
                    section, result.publishedCount(), skipCounts(result));
            } catch (RuntimeException exception) {
                log.warn("섹션 뉴스 게시 실패. section={}", section, exception);
            }
        }
    }

    private static EnumMap<SkipReason, Long> skipCounts(LaunchNewsPublishResult result) {
        return result.skips().stream().collect(Collectors.groupingBy(
            Skip::reason, () -> new EnumMap<>(SkipReason.class), Collectors.counting()));
    }
}
