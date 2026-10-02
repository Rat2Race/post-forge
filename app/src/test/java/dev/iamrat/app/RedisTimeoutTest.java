package dev.iamrat.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import dev.iamrat.support.redis.RedisGuardOperations;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Redis가 응답하지 않을 때 요청 가드가 기본 60초가 아니라 설정한 제한 시간 안에 실패하는지 본다. */
@Tag("integration")
@Testcontainers
@SpringBootTest(properties = "spring.config.import=optional:classpath:application-monitoring.yml")
@ActiveProfiles("test")
class RedisTimeoutTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private RedisGuardOperations redisGuardOperations;

    @AfterEach
    void unpause() {
        if (REDIS.getDockerClient().inspectContainerCmd(REDIS.getContainerId()).exec().getState().getPaused()) {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }

    @Test
    @DisplayName("Redis가 멈추면 요청 가드는 3초 안에 실패한다 — 장애 중 로그인·좋아요 요청이 스레드를 오래 붙잡지 않는다")
    void guardFailsFastWhenRedisHangs() {
        redisGuardOperations.incrementWithExpiry("guard:timeout:warmup", 60);
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();

        assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
            assertThatThrownBy(() -> redisGuardOperations.incrementWithExpiry("guard:timeout:test", 60))
                .isInstanceOf(RuntimeException.class));
    }
}
