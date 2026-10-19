package dev.iamrat.board.view.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// ViewCountSyncScheduler가 돌려면 스케줄링이 켜져 있어야 한다.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
