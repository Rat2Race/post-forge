package dev.iamrat.board.view.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewCountRedisKeysTest {

    @Test
    @DisplayName("조회수 캐시 key는 post ID 단위로 생성한다")
    void viewCountKey_includesPostId() {
        assertThat(ViewCountRedisKeys.viewCountKey(1L))
            .isEqualTo("post:views:1");
    }

    @Test
    @DisplayName("조회 중복 방지 key는 post ID와 account ID 단위로 생성한다")
    void viewGuardKey_includesPostAndAccountId() {
        assertThat(ViewCountRedisKeys.viewGuardKey(1L, 10L))
            .isEqualTo("post:viewed:1:10");
    }

    @Test
    @DisplayName("processing dirty key는 dirty key에서 파생한다")
    void processingDirtyKey_derivesFromDirtyKey() {
        assertThat(ViewCountRedisKeys.processingDirtyKey())
            .isEqualTo("post:views:dirty:processing");
    }
}
